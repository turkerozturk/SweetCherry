package com.turkerozturk.customlists;

import com.turkerozturk.node.*;
import com.turkerozturk.node.creation.ChildNodeService;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class CustomListsService {
    @PersistenceContext private EntityManager manager;
    private final CustomListsSettings settings;private final NodePropertiesService properties;private final ChildNodeService creation;
    public CustomListsService(CustomListsSettings settings,NodePropertiesService properties,ChildNodeService creation){this.settings=settings;this.properties=properties;this.creation=creation;}
    public record Match(long id,String name,String syntax,boolean rich) {public boolean plain(){return "plain-text".equals(syntax)&&!rich;}}
    public record Line(int number,String key,String description,String name,String status,List<Match> matches) {}
    public record View(CustomListsSettings.Names names,List<Match> mains,List<Line> lines,String status,boolean writable) {}
    @Transactional(readOnly=true)
    public View inspect(){
        var names=settings.effective();var mains=matches(names.main());
        if(mains.size()!=1)return new View(names,mains,List.of(),mains.isEmpty()?"absent":"multiple",properties.writable());
        if(!mains.get(0).plain())return new View(names,mains,List.of(),"type",properties.writable());
        String raw=Objects.toString(manager.createNativeQuery("SELECT txt FROM node WHERE node_id=:id").setParameter("id",mains.get(0).id()).getSingleResult(),"");
        if(raw.length()>1024*1024)throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
        var lines=new ArrayList<Line>();var used=new HashSet<String>();int number=0;
        for(String original:raw.split("\\R",-1)){
            number++;String line=NodeNames.clean(original);if(line.isEmpty())continue;
            if(lines.size()>=5000)throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
            boolean commented=line.startsWith("#");String value=commented?NodeNames.clean(line.substring(1)):original;
            String[] fields=value.split("\\t",2);String key=NodeNames.clean(fields[0]);String description=fields.length>1?NodeNames.clean(fields[1]):key;if(description.isEmpty())description=key;
            String name=names.template(key);String status=commented?"commented":key.isEmpty()||name.length()>255?"invalid":!used.add(key)?"duplicate":"";
            var found=status.isEmpty()?matches(name):List.<Match>of();
            if(status.isEmpty())status=found.isEmpty()?"missing":found.size()>1?"ambiguous":!found.get(0).plain()?"type":"ready";
            lines.add(new Line(number,key,description,name,status,found));
        }
        var counts=new HashMap<String,Integer>();for(var item:lines)if(!item.status().equals("commented")&&!item.status().equals("invalid"))counts.merge(item.key(),1,Integer::sum);
        var checked=new ArrayList<Line>();for(var item:lines)checked.add(!item.status().equals("commented")&&!item.status().equals("invalid")&&counts.getOrDefault(item.key(),0)>1?new Line(item.number(),item.key(),item.description(),item.name(),"duplicate",item.matches()):item);
        return new View(names,mains,List.copyOf(checked),"ready",properties.writable());
    }
    /** Exact Java comparison prevents a server's case-insensitive collation from merging different names. */
    private List<Match> matches(String name){
        @SuppressWarnings("unchecked") List<Object[]> rows=manager.createNativeQuery("SELECT node_id,name,syntax,is_richtxt FROM node WHERE name=:name ORDER BY node_id").setParameter("name",name).getResultList();
        var result=new ArrayList<Match>();for(Object[] row:rows)if(name.equals(row[1]))result.add(new Match(((Number)row[0]).longValue(),(String)row[1],(String)row[2],(((Number)row[3]).longValue()&1)!=0));return List.copyOf(result);
    }
    @Transactional
    public long createMain(){requireWritable();var names=settings.effective();if(!matches(names.main()).isEmpty())throw new ResponseStatusException(CONFLICT);return create(names.main());}
    @Transactional
    public long createTemplate(String key){
        requireWritable();var view=inspect();
        if(!view.status().equals("ready"))throw new ResponseStatusException(CONFLICT);
        var candidates=view.lines().stream().filter(l->l.key().equals(key)&&!l.status().equals("commented")).toList();
        if(candidates.size()!=1||!candidates.get(0).status().equals("missing"))throw new ResponseStatusException(CONFLICT);
        return create(candidates.get(0).name());
    }
    private void requireWritable(){if(!properties.writable())throw new AccessDeniedException("The selected database is read-only.");}
    private long create(String name){long id=creation.createTopLevel();manager.createNativeQuery("UPDATE node SET name=:name WHERE node_id=:id").setParameter("name",NodeNames.clean(name)).setParameter("id",id).executeUpdate();return id;}
}
