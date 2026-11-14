package com.turkerozturk.node.tags;

import com.turkerozturk.node.Node;
import jakarta.persistence.*;
import java.sql.*;
import java.util.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Metadata-only tag catalogue and occurrence view; does not read note text or object BLOBs. */
@Service
@Transactional(readOnly=true)
public class NodeTagService {
    @PersistenceContext private EntityManager entityManager;
    public record Options(List<String> tags,String mode,int limit,int page,String sort,String direction) {
        public Options(List<String> tags,String mode,int limit,int page){this(tags,mode,limit,page,"created","desc");}
        public Options {
            if(tags==null||tags.size()>100||!Set.of("any","all").contains(mode)||limit<1||limit>1000||page<1||page>1_000_000)
                throw new IllegalArgumentException("Invalid tag options");
            if(!Set.of("parent","node","tags","created","modified").contains(sort)||!Set.of("asc","desc").contains(direction))throw new IllegalArgumentException("Invalid sort");
            tags=List.copyOf(new LinkedHashSet<>(tags));
            for(String tag:tags)if(tag==null||tag.isEmpty()||tag.indexOf(' ')>=0||tag.length()>NodeTags.MAX_INPUT_CHARACTERS||tag.indexOf('\0')>=0)
                throw new IllegalArgumentException("Invalid tag");
        }
    }
    public record Row(long id,long contentId,String name,String icon,boolean bold,String color,boolean shared,
            boolean propertiesAvailable,String breadcrumb,List<String> tags,long parentId,String parentName,boolean parentAvailable,long created,long modified) {
        public Row(long id,long contentId,String name,String icon,boolean bold,String color,boolean shared,boolean propertiesAvailable,String breadcrumb,List<String> tags){this(id,contentId,name,icon,bold,color,shared,propertiesAvailable,breadcrumb,tags,0,"",false,0,0);}
        public String createdText(){return date(created);}
        public String modifiedText(){return date(modified);}
        private static String date(long value){if(value==0)return "—";try{return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochSecond(value));}catch(java.time.DateTimeException invalid){return "—";}}
    }
    public record View(List<String> catalogue,List<Row> rows,long taggedContents,long taggedPlaces,long matched,Options options) {
        public long pages(){return Math.max(1,(matched+options.limit()-1)/options.limit());}
        public long first(){return rows.isEmpty()?0:(long)(options.page()-1)*options.limit()+1;}
        public long last(){return rows.isEmpty()?0:first()+rows.size()-1;}
    }
    private record Meta(String name,String icon,boolean bold,String color,List<String> tags,long created,long modified) { }
    private record Branch(long id,long parent,long content,boolean shared) { }
    public List<String> catalogue(){return entityManager.unwrap(Session.class).doReturningWork(NodeTagService::catalogue);}
    static List<String> catalogue(Connection connection)throws SQLException {
        var tags=new TreeSet<String>(NodeTags.ORDER);
        // SQL DISTINCT can discard different case-sensitive values under a server's collation.
        try(var query=connection.createStatement();var rows=query.executeQuery("SELECT tags FROM node WHERE tags IS NOT NULL AND tags<>''")) {
            while(rows.next())tags.addAll(NodeTags.split(rows.getString(1)));
        }
        tags.remove(com.turkerozturk.node.TrashSql.TAG);tags.remove(com.turkerozturk.node.audit.AuditSql.TAG);
        return List.copyOf(tags);
    }
    public View inspect(Options options){return entityManager.unwrap(Session.class).doReturningWork(connection->inspect(connection,options));}
    static View inspect(Connection connection,Options options)throws SQLException {
        var nodes=new HashMap<Long,Meta>();var catalogue=new TreeSet<String>(NodeTags.ORDER);long taggedContents=0;
        try(var query=connection.createStatement();var rows=query.executeQuery("SELECT node_id,name,tags,is_ro,is_richtxt,syntax,ts_creation,ts_lastsave FROM node")) {
            while(rows.next()) {
                var display=new Node();display.setIsReadOnly16bit(rows.getLong(4));display.setIsRichText(rows.getLong(5));display.setSyntax(Objects.requireNonNullElse(rows.getString(6),"plain-text"));
                display.parseNodeIconAndReadOnlyFlag();display.parseNodeTitleColorAndBoldnessAndTextType();
                var tags=NodeTags.split(rows.getString(3)).stream().filter(tag->!tag.equals(com.turkerozturk.node.TrashSql.TAG)&&!tag.equals(com.turkerozturk.node.audit.AuditSql.TAG)).toList();if(!tags.isEmpty())taggedContents++;catalogue.addAll(tags);
                nodes.put(rows.getLong(1),new Meta(Objects.requireNonNullElse(rows.getString(2),""),display.getNodeIcon().getIconName(),display.isBoldnessBit(),String.format(Locale.ROOT,"#%06X",display.getTitleColor()),tags,rows.getLong(7),rows.getLong(8)));
            }
        }
        var branches=new TreeMap<Long,Branch>();
        try(var query=connection.createStatement();var rows=query.executeQuery("SELECT node_id,father_id,COALESCE(master_id,0) FROM children")) {
            while(rows.next()){long id=rows.getLong(1),master=rows.getLong(3);branches.put(id,new Branch(id,rows.getLong(2),master==0?id:master,master!=0));}
        }
        long taggedPlaces=0,matched=0;var result=new ArrayList<Row>();
        // Sort the complete matched occurrence set before pagination, with stable ID ties.
        var sorted=new ArrayList<>(branches.values());
        java.util.function.Function<Branch,Meta> metadata=b->nodes.get(b.content());
        java.util.function.Function<Branch,String> parent=b->{var p=branches.get(b.parent());var m=p==null?null:nodes.get(p.content());return m==null?"":m.name();};
        Comparator<Branch> order=switch(options.sort()){
            case "parent" -> Comparator.comparing(parent,NodeTags.ORDER);
            case "node" -> Comparator.comparing(b->metadata.apply(b)==null?"":metadata.apply(b).name(),NodeTags.ORDER);
            case "tags" -> Comparator.comparing(b->metadata.apply(b)==null?"":String.join(" ",metadata.apply(b).tags()),NodeTags.ORDER);
            case "modified" -> Comparator.comparingLong(b->metadata.apply(b)==null?0:metadata.apply(b).modified());
            default -> Comparator.comparingLong(b->metadata.apply(b)==null?0:metadata.apply(b).created());
        };
        if(options.direction().equals("desc"))order=order.reversed();sorted.sort(order.thenComparingLong(Branch::id));
        for(Branch branch:sorted) {
            Meta meta=nodes.get(branch.content());if(branch.id()<=0||meta==null||meta.tags().isEmpty())continue;taggedPlaces++;
            boolean accepted=options.tags().isEmpty()||(options.mode().equals("all")?meta.tags().containsAll(options.tags()):options.tags().stream().anyMatch(meta.tags()::contains));
            if(!accepted)continue;long position=matched++;
            if(position<(long)(options.page()-1)*options.limit()||result.size()>=options.limit())continue;
            Branch master=branches.get(branch.content());boolean editable=master!=null&&!master.shared();
            result.add(new Row(branch.id(),branch.content(),meta.name(),meta.icon(),meta.bold(),meta.color(),branch.shared(),editable,breadcrumb(branch.id(),branches,nodes),meta.tags(),branch.parent(),parent.apply(branch),branches.containsKey(branch.parent())&&nodes.containsKey(branches.get(branch.parent()).content()),meta.created(),meta.modified()));
        }
        return new View(List.copyOf(catalogue),List.copyOf(result),taggedContents,taggedPlaces,matched,options);
    }
    private static String breadcrumb(long id,Map<Long,Branch> branches,Map<Long,Meta> nodes) {
        var path=new ArrayDeque<String>();var visited=new HashSet<Long>();int characters=0;
        while(id!=0){if(!visited.add(id)||visited.size()>200||characters>8000){path.addFirst("… ↻");break;}
            Branch branch=branches.get(id);Meta meta=branch==null?null:nodes.get(branch.content());
            String name=meta==null?"?":meta.name();if(name.length()>500)name=name.substring(0,500)+"…";
            String part=name+" ["+id+"]";characters+=part.length();path.addFirst(part);if(branch==null)break;id=branch.parent();}
        return String.join(" / ",path);
    }
}
