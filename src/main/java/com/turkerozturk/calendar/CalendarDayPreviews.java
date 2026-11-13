package com.turkerozturk.calendar;

import java.io.StringReader;
import java.sql.*;
import java.util.*;
import javax.xml.stream.*;
import com.turkerozturk.markdown.NodeMarkup;

/** Bounded, text-only day snapshots. Never retrieves image/attachment blobs or table bodies. */
public final class CalendarDayPreviews {
    static final int SOURCE_LIMIT=65536;
    public record Preview(String text,boolean unavailable,boolean truncated,long attachments,long images,long tables,long codeboxes,boolean partialObjects) { }
    private final CalendarSettings settings;
    CalendarDayPreviews(CalendarSettings settings){this.settings=settings;}
    Map<String,Preview> read(Connection connection,List<CalendarService.Entry> entries)throws SQLException {
        var ids=entries.stream().map(CalendarService.Entry::contentId).distinct().map(Long::parseLong).toList();
        var result=new HashMap<String,Preview>();
        for(int start=0;start<ids.size();start+=200){
            var batch=ids.subList(start,Math.min(start+200,ids.size()));
            String parameters=String.join(",",Collections.nCopies(batch.size(),"?"));
            var counts=new HashMap<Long,long[]>();batch.forEach(id->counts.put(id,new long[4]));
            try(var q=connection.prepareStatement("SELECT node_id,SUM(CASE WHEN COALESCE(filename,'')<>'' THEN 1 ELSE 0 END),SUM(CASE WHEN COALESCE(filename,'')='' THEN 1 ELSE 0 END) FROM image WHERE COALESCE(anchor,'')='' AND node_id IN ("+parameters+") GROUP BY node_id")){
                bind(q,batch,1);try(var rows=q.executeQuery()){while(rows.next()){var value=counts.get(rows.getLong(1));value[0]=rows.getLong(2);value[1]=rows.getLong(3);}}
            }
            count(connection,"SELECT node_id,COUNT(*) FROM grid WHERE node_id IN ("+parameters+") GROUP BY node_id",batch,counts,2);
            count(connection,"SELECT node_id,COUNT(*) FROM codebox WHERE node_id IN ("+parameters+") GROUP BY node_id",batch,counts,3);
            try(var q=connection.prepareStatement("SELECT node_id,syntax,SUBSTR(txt,1,?) FROM node WHERE node_id IN ("+parameters+")")){
                q.setInt(1,SOURCE_LIMIT+1);bind(q,batch,2);
                try(var rows=q.executeQuery()){while(rows.next()){
                    long id=rows.getLong(1);result.put(Long.toString(id),preview(rows.getString(2),rows.getString(3),counts.get(id)));
                }}
            }
        }
        return Map.copyOf(result);
    }
    private static void bind(PreparedStatement query,List<Long> ids,int index)throws SQLException{for(long id:ids)query.setLong(index++,id);}
    private static void count(Connection c,String sql,List<Long> ids,Map<Long,long[]> counts,int index)throws SQLException{
        try(var q=c.prepareStatement(sql)){bind(q,ids,1);try(var rows=q.executeQuery()){while(rows.next())counts.get(rows.getLong(1))[index]=rows.getLong(2);}}
    }
    Preview preview(String syntax,String raw,long[] counts){
        raw=Objects.requireNonNullElse(raw,"");boolean sourceCut=raw.length()>SOURCE_LIMIT;
        String source=sourceCut?raw.substring(0,SOURCE_LIMIT):raw,text;long images=counts[1],tables=counts[2];
        try{
            if(source.isBlank())text="";
            else if("custom-colors".equals(syntax))text=richText(source,sourceCut);
            else if(NodeMarkup.supports(syntax)){
                var fragment=org.jsoup.Jsoup.parseBodyFragment(NodeMarkup.render(syntax,source));
                images+=fragment.select("img").size();tables+=fragment.select("table").size();
                fragment.select("img,script,style").remove();
                for(var br:fragment.select("br"))br.replaceWith(new org.jsoup.nodes.TextNode("\n"));
                for(var block:fragment.select("p,div,pre,li,h1,h2,h3,h4,h5,h6,tr,blockquote"))block.appendText("\n");
                text=fragment.body().wholeText().replaceAll("[\\t\\x0B\\f ]+"," ").replaceAll(" *\\n *","\n").replaceAll("\\n{3,}","\n\n");
            }else text=source;
        }catch(RuntimeException invalid){return new Preview("",true,true,counts[0],images,tables,counts[3],sourceCut&&NodeMarkup.supports(syntax));}
        text=text.replace("\r\n","\n").replace('\r','\n').strip();
        String[] lines=text.split("\n",-1);boolean cut=sourceCut||lines.length>settings.previewLines();
        if(lines.length>settings.previewLines())text=String.join("\n",Arrays.copyOf(lines,settings.previewLines()));
        int size=text.codePointCount(0,text.length());cut|=size>settings.previewCharacters();
        if(cut){int keep=Math.min(size,settings.previewCharacters()-1);text=text.substring(0,text.offsetByCodePoints(0,keep)).stripTrailing()+"…";}
        return new Preview(text,false,cut,counts[0],images,tables,counts[3],sourceCut&&NodeMarkup.supports(syntax));
    }
    private String richText(String source,boolean prefix){
        if(source.contains("<!DOCTYPE")||source.contains("<!ENTITY"))throw new IllegalArgumentException("Prohibited XML");
        var factory=XMLInputFactory.newFactory();factory.setProperty(XMLInputFactory.SUPPORT_DTD,false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
        factory.setXMLResolver((publicId,systemId,base,namespace)->{throw new XMLStreamException("External entity prohibited");});
        XMLStreamReader reader=null;var text=new StringBuilder();int depth=0;boolean inRun=false,root=false;
        try{
            reader=factory.createXMLStreamReader(new StringReader(source));
            while(reader.hasNext()){
                int type=reader.next();
                if(type==XMLStreamConstants.DTD||type==XMLStreamConstants.ENTITY_REFERENCE)throw new IllegalArgumentException("Prohibited XML");
                if(type==XMLStreamConstants.START_ELEMENT){
                    depth++;String name=reader.getLocalName();
                    if(depth==1){if(!"node".equals(name))throw new IllegalArgumentException("Invalid rich text");root=true;}
                    else if(depth==2&&"rich_text".equals(name))inRun=true;
                    else throw new IllegalArgumentException("Invalid rich text element");
                }else if(type==XMLStreamConstants.END_ELEMENT){if(depth==2)inRun=false;depth--;}
                else if(inRun&&(type==XMLStreamConstants.CHARACTERS||type==XMLStreamConstants.CDATA))text.append(reader.getText());
            }
            if(!root)throw new IllegalArgumentException("Invalid rich text");
        }catch(XMLStreamException invalid){if(!prefix||!root)throw new IllegalArgumentException("Invalid rich text",invalid);}
        finally{if(reader!=null)try{reader.close();}catch(XMLStreamException ignored){}}
        return text.toString();
    }
}
