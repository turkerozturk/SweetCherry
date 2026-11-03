package com.turkerozturk.customlists.views;

import com.turkerozturk.node.*;
import com.turkerozturk.children.ChildrenRepository;
import com.turkerozturk.node.creation.ChildNodeService;
import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import static org.springframework.http.HttpStatus.*;

@Service
public class RecordViewService {
    private final NodeService nodes;
    private final ChildrenRepository children;
    private final TemplateService templates;
    private final NodePropertiesService properties;
    private final ChildNodeService creation;
    private final RecordViewRenderer renderer;
    private final RecordViewSettings settings;
    @PersistenceContext private EntityManager manager;
    public record Choice(long id,long contentId,String name,boolean plain){}
    public record RecordChoice(long id,String name){}
    public record Catalog(long templateId,String name,String status,List<Choice> folders,List<Choice> views,List<RecordChoice> records,boolean writable){}
    public record Page(Catalog catalog,Choice view,ProjectNode record,RecordViewRenderer.Output output){}
    public RecordViewService(NodeService nodes,ChildrenRepository children,TemplateService templates,NodePropertiesService properties,ChildNodeService creation,RecordViewRenderer renderer,RecordViewSettings settings){
        this.nodes=nodes;this.children=children;this.templates=templates;this.properties=properties;this.creation=creation;this.renderer=renderer;this.settings=settings;
    }
    private Node source(long id){if(id<=0)throw new ResponseStatusException(BAD_REQUEST);Node node=nodes.findDisplayNode(id);if(node==null)throw new ResponseStatusException(NOT_FOUND);return node;}
    private List<Choice> choices(long parent){
        var result=new ArrayList<Choice>();
        for(var row:children.findByFatherIdOrderBySequenceAsc(parent)){
            Node node=nodes.findDisplayNode(row.getNodeId());if(node!=null)result.add(new Choice(row.getNodeId(),node.getNodeId(),node.getName(),"plain-text".equals(node.getSyntax())&&!node.isRichTextBit()));
            if(result.size()>64)throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
        }
        return List.copyOf(result);
    }
    private List<Choice> folders(long template){
        var result=new ArrayList<Choice>();for(var row:children.findByFatherIdOrderBySequenceAsc(template)){
            Node node=nodes.findDisplayNode(row.getNodeId());if(node!=null&&"_views".equals(node.getName()))result.add(new Choice(row.getNodeId(),node.getNodeId(),node.getName(),true));
        }return List.copyOf(result);
    }
    private Catalog catalog(long id,boolean includeRecords){
        Node node=source(id);var folders=folders(id);var views=folders.size()==1?choices(folders.get(0).id()):List.<Choice>of();
        var records=new ArrayList<RecordChoice>();
        if(includeRecords)for(Node record:nodes.getNodesByTags(node.getName()))if(Objects.equals(record.getTags(),node.getName()))records.add(new RecordChoice(record.getNodeId(),record.getName()));
        records.sort(Comparator.comparing(RecordChoice::name,Comparator.nullsFirst(Comparator.naturalOrder())).thenComparingLong(RecordChoice::id));
        return new Catalog(id,node.getName(),folders.isEmpty()?"missing":folders.size()>1?"multiple":"ready",folders,views,List.copyOf(records),properties.writable());
    }
    @Transactional(readOnly=true)
    public Catalog catalog(long id){return catalog(id,true);}
    @Transactional(readOnly=true)
    public Page render(long templateId,long viewId,long recordId,String token,Locale locale){
        var catalog=catalog(templateId,false);
        if(!catalog.status().equals("ready"))throw new ResponseStatusException(CONFLICT);
        var view=catalog.views().stream().filter(v->v.id()==viewId).findFirst().orElseThrow(()->new ResponseStatusException(NOT_FOUND));
        if(!view.plain())throw new ResponseStatusException(CONFLICT);
        var record=templates.getRecord(templateId,recordId,token);
        var output=renderer.render(source(viewId).getTxt(),source(templateId),view.name(),record,token,locale,Instant.now());
        return new Page(catalog,view,record,output);
    }
    /** Explicit POST only; one transaction creates ordinary CTB nodes without schema changes. */
    @Transactional
    public long createExample(long templateId){
        if(!properties.writable())throw new AccessDeniedException("Read-only connection");
        var template=source(templateId);var folders=folders(templateId);
        if(folders.size()>1)throw new ResponseStatusException(CONFLICT);
        long folder=folders.isEmpty()?create(templateId,"_views",""):folders.get(0).id();
        if(choices(folder).stream().anyMatch(v->"Example layout".equals(v.name())))throw new ResponseStatusException(CONFLICT);
        var source=new StringBuilder("<article style=\"max-width:1000px;margin:auto\"><h1>{{record.name}}</h1><p>{{record.id}} · {{now}}</p><p>{{record.breadcrumbs}}</p>");
        var definition=new TemplateNode(template);
        for(var field:definition.getDataLabels().entrySet()){
            source.append("<section style=\"border:1px solid #ccc;padding:12px;margin:12px 0\"><h2>").append(org.jsoup.nodes.Entities.escape(field.getValue())).append("</h2>");
            if(field.getKey().indexOf('{')<0 && field.getKey().indexOf('}')<0 && field.getKey().length()<=250)source.append("{{field.").append(org.jsoup.nodes.Entities.escape(field.getKey())).append("}}");
            else source.append(org.jsoup.nodes.Entities.escape(field.getKey())).append(" [unsupported placeholder key]");
            source.append("</section>");
        }
        source.append("</article>");if(source.length()>settings.getMaxDesignCharacters())throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
        return create(folder,"Example layout",source.toString());
    }
    private long create(long parent,String name,String text){
        long id=creation.create(parent);
        int updated=manager.createNativeQuery("UPDATE node SET name=:name,txt=:text,syntax='plain-text' WHERE node_id=:id").setParameter("name",name).setParameter("text",text).setParameter("id",id).executeUpdate();
        if(updated!=1)throw new IllegalStateException("Created layout node not found");return id;
    }
}
