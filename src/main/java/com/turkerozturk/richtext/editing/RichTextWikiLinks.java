package com.turkerozturk.richtext.editing;

import com.turkerozturk.richtext.experimental.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Resolves editor text only. Persistent results use ordinary CherryTree node links. */
final class RichTextWikiLinks {
    private static final Pattern TOKEN=Pattern.compile("(?<!\\[)\\[\\[([^\\[\\]\\r\\n]{1,1024})]](?!])");
    record Candidate(String id,String name,String breadcrumb,boolean shared){}
    record Issue(String name,String kind,List<Candidate> candidates,boolean truncated){}
    record Preview(Map<String,String> choices,List<Issue> issues,int occurrences){}
    record Result(RichTextDocument document,Preview preview){}
    private record Place(long id,long parent,String name,boolean shared){}
    private record Token(int start,int end,String name,String label){}
    private static boolean eligible(RichTextDocument.TextRun run){return !run.attributes().containsKey("link")&&!run.attributes().containsKey(ProtectedRichTextCodec.OBJECT_ATTRIBUTE);}
    private static List<Token> tokens(String text){
        var result=new ArrayList<Token>();var matcher=TOKEN.matcher(text);
        while(matcher.find()){
            int slashes=0;for(int at=matcher.start()-1;at>=0&&text.charAt(at)=='\\';at--)slashes++;
            if(slashes%2==1)continue;
            String[] parts=matcher.group(1).split("\\|",-1);if(parts.length>2)continue;
            String name=parts[0].strip(),label=parts.length==1?name:parts[1];
            if(name.isEmpty()||name.length()>255||label.isBlank()||name.codePoints().anyMatch(Character::isISOControl))continue;
            result.add(new Token(matcher.start(),matcher.end(),name,label));if(result.size()>200)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"wikiLimit");
        }return result;
    }
    private static List<List<RichTextDocument.TextRun>> groups(RichTextDocument doc){
        var result=new ArrayList<List<RichTextDocument.TextRun>>();List<RichTextDocument.TextRun> current=null;
        for(var run:doc.runs()){
            if(!eligible(run)){result.add(List.of(run));current=null;}
            else{if(current==null){current=new ArrayList<>();result.add(current);}current.add(run);}
        }return result;
    }
    static Map<String,String> decode(String json){
        try{
            if(json==null||json.length()>64000)throw new IllegalArgumentException();
            var tree=tools.jackson.databind.json.JsonMapper.builder().build().readTree(json);
            if(tree==null||!tree.isObject()||tree.size()>50)throw new IllegalArgumentException();var values=new LinkedHashMap<String,String>();
            for(var item:tree.properties()){
                String value=item.getValue().asString();if(!item.getValue().isString()||item.getKey().length()>255||!value.matches("0|[1-9][0-9]{0,18}"))throw new IllegalArgumentException();
                values.put(item.getKey(),value);
            }return values;
        }catch(RuntimeException invalid){throw new IllegalArgumentException("Invalid wiki choices",invalid);}
    }
    static Result resolve(Connection c,RichTextDocument doc,Map<String,String> choices,boolean keepUnresolved,boolean preview)throws SQLException{
        var groups=groups(doc);var names=new LinkedHashSet<String>();int occurrences=0;
        for(var group:groups)if(eligible(group.get(0))){for(var token:tokens(join(group))){names.add(token.name());if(++occurrences>200||names.size()>50)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"wikiLimit");}}
        if(!names.containsAll(choices.keySet()))throw new IllegalArgumentException("Unknown wiki choice");
        if(names.isEmpty())return new Result(doc,new Preview(Map.of(),List.of(),0));
        var places=new LinkedHashMap<Long,Place>();var matches=new HashMap<String,List<Place>>();
        try(var q=c.prepareStatement("SELECT c.node_id,c.father_id,COALESCE(c.master_id,0),n.name FROM children c JOIN node n ON n.node_id=CASE WHEN COALESCE(c.master_id,0)=0 THEN c.node_id ELSE c.master_id END ORDER BY c.node_id")){
            q.setMaxRows(20001);try(var r=q.executeQuery()){while(r.next()){
                if(places.size()>=20000)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"wikiLimit");
                var place=new Place(r.getLong(1),r.getLong(2),Objects.requireNonNullElse(r.getString(4),""),r.getLong(3)!=0);places.put(place.id(),place);
                if(names.contains(place.name()))matches.computeIfAbsent(place.name(),key->new ArrayList<>()).add(place);
            }}
        }
        var selected=new LinkedHashMap<String,String>();var issues=new ArrayList<Issue>();
        for(String name:names){var items=matches.getOrDefault(name,List.of());String wanted=choices.get(name);
            if(wanted!=null){
                if(wanted.equals("0")){if(!keepUnresolved&&!preview)throw new ResponseStatusException(HttpStatus.CONFLICT,"wikiUnresolved");selected.put(name,"0");}
                else{if(items.stream().noneMatch(p->Long.toString(p.id()).equals(wanted)))throw new ResponseStatusException(HttpStatus.CONFLICT,"wikiTargetChanged");selected.put(name,wanted);}
            }else if(items.size()==1)selected.put(name,Long.toString(items.get(0).id()));
            else{
                if(!preview&&!keepUnresolved)throw new ResponseStatusException(HttpStatus.CONFLICT,"wikiUnresolved");
                selected.put(name,"0");var candidates=items.stream().limit(200).map(p->new Candidate(Long.toString(p.id()),p.name(),breadcrumb(p,places),p.shared())).toList();issues.add(new Issue(name,items.isEmpty()?"missing":"ambiguous",candidates,items.size()>200));
            }
        }
        var output=new ArrayList<RichTextDocument.TextRun>();
        for(var group:groups){
            if(!eligible(group.get(0))){for(var run:group)append(output,run.text(),run.attributes());continue;}
            String text=join(group);int from=0;
            for(var token:tokens(text)){
                String target=selected.get(token.name());if("0".equals(target))continue;
                copy(group,from,token.start(),output);var attributes=new LinkedHashMap<>(attributesAt(group,token.start()+2));attributes.put("link","node "+target);
                // Preserve each label character's original formatting when no alias is supplied.
                int pipe=text.indexOf('|',token.start()+2);boolean alias=pipe>=0&&pipe<token.end()-2;
                if(alias)append(output,token.label(),attributes);
                else{
                    int start=token.start()+2;while(start<token.end()-2&&Character.isWhitespace(text.charAt(start)))start++;
                    int end=token.end()-2;while(end>start&&Character.isWhitespace(text.charAt(end-1)))end--;
                    var label=new ArrayList<RichTextDocument.TextRun>();copy(group,start,end,label);for(var run:label){var style=new LinkedHashMap<>(run.attributes());style.put("link","node "+target);append(output,run.text(),style);}
                }
                from=token.end();
            }copy(group,from,text.length(),output);
        }
        return new Result(new RichTextDocument(output),new Preview(Map.copyOf(selected),List.copyOf(issues),occurrences));
    }
    private static String join(List<RichTextDocument.TextRun> group){var result=new StringBuilder();group.forEach(r->result.append(r.text()));return result.toString();}
    private static Map<String,String> attributesAt(List<RichTextDocument.TextRun> group,int index){int pos=0;for(var run:group){if(index<pos+run.text().length())return run.attributes();pos+=run.text().length();}return Map.of();}
    private static void copy(List<RichTextDocument.TextRun> group,int from,int to,List<RichTextDocument.TextRun> out){int pos=0;for(var run:group){int a=Math.max(0,from-pos),b=Math.min(run.text().length(),to-pos);if(a<b)append(out,run.text().substring(a,b),run.attributes());pos+=run.text().length();}}
    private static void append(List<RichTextDocument.TextRun> out,String text,Map<String,String> attributes){if(text.isEmpty())return;int offset=out.isEmpty()?0:out.get(out.size()-1).textOffset()+out.get(out.size()-1).characterCount();out.add(new RichTextDocument.TextRun(text,attributes,offset));}
    private static String breadcrumb(Place place,Map<Long,Place> places){var path=new ArrayDeque<String>();var seen=new HashSet<Long>();int length=0;while(place!=null){if(!seen.add(place.id())||seen.size()>100||length>8000){path.addFirst("…");break;}String label=place.name()+" ["+place.id()+"]";path.addFirst(label);length+=label.length();place=places.get(place.parent());}return String.join(" / ",path);}
}
