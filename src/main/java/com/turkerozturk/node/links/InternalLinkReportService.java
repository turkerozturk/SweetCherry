package com.turkerozturk.node.links;

import com.turkerozturk.node.Node;
import com.turkerozturk.jdbc.JdbcIdentifiers;
import com.turkerozturk.richtext.experimental.RichTextXmlReader;
import jakarta.persistence.*;
import java.sql.*;
import java.util.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** On-demand metadata/text inspection. Does not render content, load BLOBs or write a CTB. */
@Service
@Transactional(readOnly = true)
public class InternalLinkReportService {
    @PersistenceContext private EntityManager entityManager;
    static final int MAX_XML_CHARACTERS = 2_000_000;
    public record Options(String query, String status, int limit, int page) {
        public Options {
            if(query==null || query.length()>500 || !Set.of("all","broken","ok").contains(status)
                    || limit<1 || limit>1000 || page<1 || page>1_000_000) throw new IllegalArgumentException("Invalid report options");
        }
    }
    public record Place(long id, String name, String icon, boolean bold, String color, boolean shared,
            boolean available, String breadcrumb) { }
    public record Row(Place source, String kind, int offset, String label, String rawLink, String targetId,
            Place target, String anchor, String fragment, String status) { }
    public record Issue(long contentId, Place source, String reason) { }
    public record Report(List<Row> rows, long links, long sourceContents, long sourcePlaces, long missingNodes,
            long missingAnchors, long invalidLinks, long matched, long unreadable, List<Issue> issues, Options options) {
        public long broken(){return missingNodes+missingAnchors+invalidLinks;}
        public long pages(){return Math.max(1,(matched+options.limit()-1)/options.limit());}
        public long first(){return rows.isEmpty()?0:(long)(options.page()-1)*options.limit()+1;}
        public long last(){return rows.isEmpty()?0:first()+rows.size()-1;}
    }
    private record Meta(String name,String icon,boolean bold,String color,String syntax) { }
    private record Branch(long id,long parent,long content,boolean shared) { }
    private record Link(String kind,int offset,String label,String value) { }
    private record Parsed(String id,Long number,String anchor) { }
    public Report inspect(Options options) {
        return entityManager.unwrap(Session.class).doReturningWork(connection->inspect(connection,options));
    }
    static Report inspect(Connection connection, Options options) throws SQLException {
        var metadata=new HashMap<Long,Meta>();
        try(var query=connection.createStatement();var rows=query.executeQuery("SELECT node_id,name,is_ro,is_richtxt,syntax FROM node")) {
            while(rows.next()) {
                var display=new Node();display.setIsReadOnly16bit(rows.getLong(3));display.setIsRichText(rows.getLong(4));
                String syntax=Objects.requireNonNullElse(rows.getString(5),"plain-text");display.setSyntax(syntax);
                display.parseNodeIconAndReadOnlyFlag();display.parseNodeTitleColorAndBoldnessAndTextType();
                metadata.put(rows.getLong(1),new Meta(Objects.requireNonNullElse(rows.getString(2),""),display.getNodeIcon().getIconName(),
                        display.isBoldnessBit(),String.format(Locale.ROOT,"#%06X",display.getTitleColor()),syntax));
            }
        }
        var branches=new HashMap<Long,Branch>();var contentPlaces=new HashMap<Long,List<Long>>();
        try(var query=connection.createStatement();var rows=query.executeQuery("SELECT node_id,father_id,COALESCE(master_id,0) FROM children ORDER BY node_id")) {
            while(rows.next()) {
                long id=rows.getLong(1),master=rows.getLong(3),content=master==0?id:master;
                branches.put(id,new Branch(id,rows.getLong(2),content,master!=0));
                contentPlaces.computeIfAbsent(content,key->new ArrayList<>()).add(id);
            }
        }
        var anchors=new HashMap<Long,Set<String>>();var objectLinks=new HashMap<Long,List<Link>>();
        String sql="SELECT node_id,"+JdbcIdentifiers.quote(connection,"offset")+",filename,anchor,link FROM image WHERE "
                + "(anchor IS NOT NULL AND anchor<>'') OR (link IS NOT NULL AND link<>'')";
        try(var query=connection.createStatement();var rows=query.executeQuery(sql)) {
            while(rows.next()) {
                long id=rows.getLong(1);String anchor=rows.getString(4),value=rows.getString(5);
                if(anchor!=null&&!anchor.isEmpty())anchors.computeIfAbsent(id,key->new HashSet<>()).add(anchor);
                if(internal(value))objectLinks.computeIfAbsent(id,key->new ArrayList<>()).add(new Link("object",rows.getInt(2),
                        Objects.requireNonNullElse(rows.getString(3),""),value));
            }
        }
        objectLinks.values().forEach(links->links.sort(Comparator.comparingInt(Link::offset)));
        var places=new HashMap<Long,Place>();
        for(long id:branches.keySet())places.put(id,place(id,branches,metadata,false));
        var result=new Collector(options,places,branches,metadata,anchors);
        var reader=new RichTextXmlReader();
        try(var query=connection.prepareStatement("SELECT node_id,txt FROM node WHERE syntax='custom-colors' ORDER BY node_id")) {
            query.setFetchSize(50);
            try(var rows=query.executeQuery()) {
                while(rows.next()) {
                    long id=rows.getLong(1);String xml=rows.getString(2);
                    var sources=contentPlaces.getOrDefault(id,List.of()).stream().map(places::get).toList();
                    if(sources.isEmpty()) {
                        var meta=metadata.get(id);
                        sources=List.of(new Place(id,meta.name(),meta.icon(),meta.bold(),meta.color(),false,false,meta.name()+" ["+id+"]"));
                    }
                    boolean found=false;
                    if(xml!=null&&!xml.isBlank()) {
                        if(xml.length()>MAX_XML_CHARACTERS)result.issue(id,sources.get(0),"tooLarge");
                        else try {
                            var runs=reader.read(xml).runs();
                            for(int i=0;i<runs.size();) {
                                var first=runs.get(i);String link=first.attributes().get("link");int end=i+1;
                                var label=new StringBuilder(first.text());
                                if(link!=null)while(end<runs.size()&&link.equals(runs.get(end).attributes().get("link")))label.append(runs.get(end++).text());
                                if(internal(link)){found=true;result.add(sources,new Link("text",first.textOffset(),label.toString(),link));}
                                i=end;
                            }
                        }catch(IllegalArgumentException invalid){result.issue(id,sources.get(0),"invalidXml");}
                    }
                    for(var link:objectLinks.getOrDefault(id,List.of())){found=true;result.add(sources,link);}
                    if(found)result.sourceContents++;
                }
            }
        }
        return result.report();
    }
    private static boolean internal(String value){return value!=null&&(value.equals("node")||value.startsWith("node ")||value.startsWith("node\t")||value.startsWith("node\n")||value.startsWith("node\r"));}
    private static Place place(long id,Map<Long,Branch> branches,Map<Long,Meta> metadata,boolean withBreadcrumb) {
        Branch branch=branches.get(id);Meta meta=metadata.get(branch.content());
        if(!withBreadcrumb)return new Place(id,meta==null?"":meta.name(),meta==null?"ct_code":meta.icon(),meta!=null&&meta.bold(),meta==null?"#000000":meta.color(),branch.shared(),meta!=null,"");
        var path=new ArrayDeque<String>();var seen=new HashSet<Long>();long parent=id;int characters=0;
        while(parent!=0) {
            if(!seen.add(parent)||seen.size()>200||characters>8000){path.addFirst("… ↻");break;}
            Branch current=branches.get(parent);Meta item=current==null?null:metadata.get(current.content());
            String part=(item==null?"?":shortText(item.name(),500))+" ["+parent+"]";path.addFirst(part);characters+=part.length();
            if(current==null)break;parent=current.parent();
        }
        return new Place(id,meta==null?"":meta.name(),meta==null?"ct_code":meta.icon(),meta!=null&&meta.bold(),meta==null?"#000000":meta.color(),branch.shared(),meta!=null,String.join(" / ",path));
    }
    private static Parsed parse(String value) {
        var matcher=java.util.regex.Pattern.compile("node ([1-9][0-9]{0,18})(?: (.+))?").matcher(value);
        if(!matcher.matches()||value.codePoints().anyMatch(Character::isISOControl))return new Parsed(shortText(value,200),null,"");
        try{return new Parsed(matcher.group(1),Long.parseLong(matcher.group(1)),Objects.requireNonNullElse(matcher.group(2),""));}
        catch(NumberFormatException invalid){return new Parsed(matcher.group(1),null,"");}
    }
    private static String normalize(String value){return java.text.Normalizer.normalize(value.toLowerCase(Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('ı','i');}
    private static String shortText(String value,int maximum){return value.length()>maximum?value.substring(0,maximum)+"…":value;}
    private static final class Collector {
        final Map<Long,Meta> metadata;final Map<Long,Place> detailed=new HashMap<>();
        final Options options;final Map<Long,Place> places;final Map<Long,Branch> branches;final Map<Long,Set<String>> anchors;
        final List<Row> rows=new ArrayList<>();final List<Issue> issues=new ArrayList<>();final Set<Long> sources=new HashSet<>();
        final String query;long links,sourceContents,missingNodes,missingAnchors,invalidLinks,matched,unreadable;
        Collector(Options options,Map<Long,Place> places,Map<Long,Branch> branches,Map<Long,Meta> metadata,Map<Long,Set<String>> anchors){this.metadata=metadata;this.options=options;this.places=places;this.branches=branches;this.anchors=anchors;query=normalize(options.query());}
        Place details(Place value){return value==null||!branches.containsKey(value.id())?value:detailed.computeIfAbsent(value.id(),id->place(id,branches,metadata,true));}
        void issue(long id,Place source,String reason){unreadable++;if(issues.size()<100)issues.add(new Issue(id,details(source),reason));}
        void add(List<Place> sourcePlaces,Link link) {
            Parsed parsed=parse(link.value());Place target=parsed.number()==null?null:places.get(parsed.number());
            if(target==null&&parsed.number()!=null&&metadata.containsKey(parsed.number())) {
                Meta meta=metadata.get(parsed.number());target=new Place(parsed.number(),meta.name(),meta.icon(),meta.bold(),meta.color(),false,false,meta.name()+" ["+parsed.number()+"]");
            }
            String state=parsed.number()==null?"invalid":target==null||!target.available()?"missingNode":
                    !parsed.anchor().isEmpty()&&!anchors.getOrDefault(branches.get(target.id()).content(),Set.of()).contains(parsed.anchor())?"missingAnchor":"ok";
            for(Place source:sourcePlaces) {
                links++;if(source.available())sources.add(source.id());
                switch(state){case "missingNode"->missingNodes++;case "missingAnchor"->missingAnchors++;case "invalid"->invalidLinks++;default->{}}
                if(options.status().equals("broken")&&state.equals("ok")||options.status().equals("ok")&&!state.equals("ok"))continue;
                if(!query.isEmpty()&&!normalize(source.name()+" "+source.id()+" "+(target==null?"":target.name())+" "+parsed.id()+" "+parsed.anchor()+" "+link.label()).contains(query))continue;
                long position=matched++;
                if(position<(long)(options.page()-1)*options.limit()||rows.size()>=options.limit())continue;
                String fragment=parsed.anchor().isEmpty()||!state.equals("ok")?"":"#"+java.net.URLEncoder.encode(parsed.anchor(),java.nio.charset.StandardCharsets.UTF_8).replace("+","%20");
                rows.add(new Row(details(source),link.kind(),link.offset(),shortText(link.label(),2000),shortText(link.value(),4096),parsed.id(),details(target),parsed.anchor(),fragment,state));
            }
        }
        Report report(){return new Report(List.copyOf(rows),links,sourceContents,sources.size(),missingNodes,missingAnchors,invalidLinks,matched,unreadable,List.copyOf(issues),options);}
    }
}
