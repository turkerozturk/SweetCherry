package com.turkerozturk.importing.mindmap;

import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.importing.CtbTreeWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.w3c.dom.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/** An immutable, bounded preview. Only direct map/node children are hierarchy, never styles/hooks. */
public record MindMapImportData(List<Entry> nodes, Map<String,Integer> warnings) {
    public static final int MAX_BYTES=1024*1024, MAX_NODES=10000, MAX_DEPTH=128;
    public record Entry(String name,String text,int parent,int sequence,long titleBits,int icon,long created,long modified) { }
    private record Frame(Element node,int parent,int sequence,int depth) { }
    public MindMapImportData { nodes=List.copyOf(nodes); warnings=Collections.unmodifiableMap(new LinkedHashMap<>(warnings)); }
    public String rootName(){ return nodes.get(0).name(); }
    public int count(){return nodes.size();}

    public static MindMapImportData read(byte[] bytes) {
        if(bytes.length==0)throw problem("file");
        if(bytes.length>MAX_BYTES)throw problem("limit");
        try {
            var factory=DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth",256);
            factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
            var builder=factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler(){
                @Override public void error(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException {throw e;}
                @Override public void fatalError(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException {throw e;}
            });
            Document document=builder.parse(new ByteArrayInputStream(bytes));
            Element map=document.getDocumentElement();
            if(!"map".equals(map.getTagName()))throw problem("xml");
            var roots=children(map,"node"); if(roots.size()!=1)throw problem("xml");
            // Legacy SweetCherry files identify themselves in a direct map comment.
            boolean own=false;
            for(var item=map.getFirstChild();item!=null;item=item.getNextSibling())
                if(item.getNodeType()==org.w3c.dom.Node.COMMENT_NODE && item.getNodeValue().startsWith("Exported by SweetCherry:"))own=true;
            var entries=new ArrayList<Entry>();var warnings=new LinkedHashMap<String,Integer>();
            var stack=new ArrayDeque<Frame>();stack.push(new Frame(roots.get(0),-1,1,0));
            long now=Instant.now().getEpochSecond();
            while(!stack.isEmpty()) {
                Frame frame=stack.pop();Element node=frame.node();
                if(frame.depth()>MAX_DEPTH || entries.size()>=MAX_NODES)throw problem("limit");
                String name=node.hasAttribute("TEXT")?node.getAttribute("TEXT"):"";
                String note="",details="";
                for(Element content:children(node,"richcontent")) {
                    String value=contentText(content);
                    switch(content.getAttribute("TYPE").toUpperCase(Locale.ROOT)) {
                        case "NODE" -> {if(!value.isBlank())name=value;}
                        case "NOTE" -> note=join(note,value);
                        case "DETAILS" -> details=join(details,value);
                        default -> warn(warnings,"unsupported");
                    }
                }
                String text=details.isEmpty()?note:note.isEmpty()?details:"Details:\n"+details+"\n\nNote:\n"+note;
                // Preserve links/attributes as inert text; never load images, external maps or execute formulas/hooks.
                if(node.hasAttribute("LINK"))text=join(text,"[Link] "+node.getAttribute("LINK"));
                for(Element attribute:children(node,"attribute"))
                    if(!attribute.getAttribute("NAME").equals("sweetcherry.node-icon"))
                        text=join(text,attribute.getAttribute("NAME")+": "+attribute.getAttribute("VALUE"));
                for(Element link:children(node,"arrowlink"))text=join(text,"[Connection] "+link.getAttribute("DESTINATION"));
                if(node.hasAttribute("ENCRYPTED_CONTENT")) {warn(warnings,"encrypted");text=join(text,"[Encrypted Freeplane content was not imported.]");}
                if(name.isBlank()){ name="(Untitled)";warn(warnings,"titles"); }
                if(name.codePointCount(0,name.length())>255){text=join("[Original title]\n"+name,text);name=name.substring(0,name.offsetByCodePoints(0,255));warn(warnings,"titles");}
                long bits=0;
                if(node.getAttribute("COLOR").matches("#[a-fA-F0-9]{6}")) bits=Long.parseLong(node.getAttribute("COLOR").substring(1),16)<<3;
                for(Element font:children(node,"font"))if(Boolean.parseBoolean(font.getAttribute("BOLD")))bits|=2;
                int icon=icon(node,own && frame.parent()==-1,warnings);
                if(!children(node,"icon").isEmpty())warn(warnings,"icons");
                for(Element hook:children(node,"hook")) {
                    if("ExternalObject".equals(hook.getAttribute("NAME"))) {
                        if(!isIconUri(hook.getAttribute("URI"))) {warn(warnings,"images");text=join(text,"[External object] "+hook.getAttribute("URI"));}
                    } else warn(warnings,"unsupported");
                }
                if(node.hasAttribute("STYLE_REF") || !children(node,"cloud").isEmpty() || !children(node,"edge").isEmpty())warn(warnings,"unsupported");
                long created=timestamp(node.getAttribute("CREATED"),now),modified=timestamp(node.getAttribute("MODIFIED"),created);
                int index=entries.size();entries.add(new Entry(name,text,frame.parent(),frame.sequence(),bits,icon,created,modified));
                var descendants=children(node,"node");
                for(int i=descendants.size()-1;i>=0;i--)stack.push(new Frame(descendants.get(i),index,i+1,frame.depth()+1));
            }
            return new MindMapImportData(entries,warnings);
        }catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw problem("xml");}
    }
    private static long timestamp(String value,long fallback){
        try {long millis=Long.parseLong(value);return millis>0?millis/1000:fallback;}catch(NumberFormatException e){return fallback;}
    }
    private static int icon(Element node,boolean legacyRoot,Map<String,Integer>warnings){
        for(Element attr:children(node,"attribute"))if("sweetcherry.node-icon".equals(attr.getAttribute("NAME"))) {
            for(NodeIcon icon:NodeIcon.values())if(icon.getIconName().equals(attr.getAttribute("VALUE")))return icon.getIconIdIn16bit();
            warn(warnings,"icons");return 0;
        }
        var hooks=children(node,"hook").stream().filter(h->"ExternalObject".equals(h.getAttribute("NAME"))).toList();
        // Old exporters put the database emblem first on the root, then the real node icon (if any).
        for(int i=legacyRoot?1:0;i<hooks.size();i++) {
            String uri=hooks.get(i).getAttribute("URI");
            for(NodeIcon icon:NodeIcon.values())if(uri.equals("ctbicons/"+icon.getIconName()+".png"))return icon.getIconIdIn16bit();
        }
        return 0;
    }
    private static boolean isIconUri(String uri){for(NodeIcon icon:NodeIcon.values())if(uri.equals("ctbicons/"+icon.getIconName()+".png"))return true;return false;}
    private static List<Element> children(Element parent,String tag){
        var result=new ArrayList<Element>();
        for(var n=parent.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element e && e.getTagName().equals(tag))result.add(e);
        return result;
    }
    private static String contentText(Element content)throws Exception {
        var bodies=content.getElementsByTagName("body");Element body=bodies.getLength()>0?(Element)bodies.item(0):content;
        // SweetCherry's plain-text export uses one pre: retain all spaces/newlines, including empty content.
        var parts=new ArrayList<Element>();
        for(var n=body.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element e)parts.add(e);
        if(parts.size()==1 && parts.get(0).getTagName().equals("pre"))return parts.get(0).getTextContent();
        var tf=javax.xml.transform.TransformerFactory.newInstance();tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
        var writer=new StringWriter();tf.newTransformer().transform(new javax.xml.transform.dom.DOMSource(body),new javax.xml.transform.stream.StreamResult(writer));
        var html=Jsoup.parse(writer.toString());html.select("script,style,head").remove();
        var result=new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor(){
            public void head(org.jsoup.nodes.Node n,int depth){
                if(n instanceof TextNode t){
                    String s=t.text();
                    if(!s.isBlank())result.append(s);
                    else if(!result.isEmpty()&&!Character.isWhitespace(result.charAt(result.length()-1)))result.append(' ');
                }
                else if(n instanceof org.jsoup.nodes.Element e){
                    if(Set.of("p","div","li","tr","h1","h2","h3","h4","h5","h6","pre","br").contains(e.tagName()))line(result);
                    if(e.tagName().equals("li"))result.append("• ");
                    if(e.tagName().equals("img"))result.append("[Image: ").append(e.attr("alt")).append(" ").append(e.attr("src")).append("]");
                }
            }
            public void tail(org.jsoup.nodes.Node n,int depth){
                if(n instanceof org.jsoup.nodes.Element e){
                    if(e.tagName().equals("a")&&!e.attr("href").isEmpty())result.append(" (").append(e.attr("href")).append(")");
                    if(Set.of("p","div","li","tr","h1","h2","h3","h4","h5","h6","pre").contains(e.tagName()))line(result);
                    if(e.tagName().equals("td")||e.tagName().equals("th"))result.append('\t');
                }
            }
        },html.body());
        return result.toString().replace('\u00a0',' ').strip();
    }
    private static void line(StringBuilder text){
        while(!text.isEmpty() && (text.charAt(text.length()-1)==' '||text.charAt(text.length()-1)=='\t'))text.setLength(text.length()-1);
        if(!text.isEmpty()&&text.charAt(text.length()-1)!='\n')text.append('\n');}
    private static String join(String a,String b){return a.isEmpty()?b:b.isEmpty()?a:a+"\n\n"+b;}
    private static void warn(Map<String,Integer>warnings,String key){warnings.merge(key,1,Integer::sum);}
    private static IllegalArgumentException problem(String key){return new IllegalArgumentException(key);}

    int insert(Connection connection) throws SQLException {
        var tree = nodes.stream().map(entry -> new CtbTreeWriter.Entry(entry.name(), entry.text(), "",
                entry.parent(), entry.sequence(), entry.titleBits(), entry.icon(), entry.created(), entry.modified())).toList();
        return CtbTreeWriter.insert(connection, tree, false).count();
    }
}
