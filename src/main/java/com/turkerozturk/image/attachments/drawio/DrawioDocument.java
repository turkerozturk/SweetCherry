package com.turkerozturk.image.attachments.drawio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.helpers.DefaultHandler;
import static com.turkerozturk.image.attachments.drawio.DrawioService.*;

/** Converts untrusted models to bounded, inert graph geometry and constrained HTML labels. No URLs are fetched. */
final class DrawioDocument {
    static final int PAGE_BYTES=DrawioSettings.defaults().pageBytes(),TOTAL_BYTES=DrawioSettings.defaults().totalBytes(),MAX_PAGES=DrawioSettings.defaults().pages(),MAX_CELLS=DrawioSettings.defaults().cells();
    private final DrawioSettings settings;
    private DrawioDocument(DrawioSettings settings){this.settings=settings;}
    private static Problem limit(String key){return new Problem("large",key);}
    private static Problem invalid(){return new Problem("invalid");}
    static Preview parse(byte[] bytes,String filename,String nodeName){return parse(bytes,filename,nodeName,DrawioSettings.defaults());}
    static Preview parse(byte[] bytes,String filename,String nodeName,DrawioSettings settings){return new DrawioDocument(settings).document(bytes,filename,nodeName);}
    private Preview document(byte[] bytes,String filename,String nodeName){
        if(!supports(filename))throw new Problem("unsupported");if(bytes.length>settings.fileBytes())throw limit("max-file-bytes");
        try{
            Document doc=xml(bytes);Element outer=doc.getDocumentElement();var pages=new ArrayList<Page>();long expanded=0,cells=0;
            if("mxGraphModel".equals(outer.getTagName())){if(bytes.length>settings.pageBytes())throw limit("max-page-bytes");if(outer.getElementsByTagName("mxCell").getLength()>settings.totalCells())throw limit("max-total-cells");String model=clean(outer);if(Math.max(bytes.length,model.getBytes(StandardCharsets.UTF_8).length)>settings.totalBytes())throw limit("max-total-bytes");return new Preview(filename,nodeName,List.of(new Page("1",model)),settings.pageBytes());}
            if(!"mxfile".equals(outer.getTagName()))throw invalid();
            for(Node child=outer.getFirstChild();child!=null;child=child.getNextSibling()){
                if(!(child instanceof Element diagram)||!"diagram".equals(diagram.getTagName()))continue;
                if(pages.size()>=settings.pages())throw limit("max-pages");
                int sourceBytes=0;Element model=null;for(Node n=diagram.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element e){if(model!=null||!"mxGraphModel".equals(e.getTagName()))throw invalid();model=e;}
                if(model!=null){sourceBytes=serialize(model).getBytes(StandardCharsets.UTF_8).length;if(sourceBytes>settings.pageBytes())throw limit("max-page-bytes");}
                if(model==null){String text=diagram.getTextContent().strip();byte[] data;
                    if(text.startsWith("<"))data=text.getBytes(StandardCharsets.UTF_8);
                    else{byte[] compressed=Base64.getDecoder().decode(text.replaceAll("\\s+",""));var inflater=new Inflater(true);
                        try(var in=new InflaterInputStream(new ByteArrayInputStream(compressed),inflater)){
                            int encodedBudget=settings.pageBytes()*3;byte[] encoded=in.readNBytes(encodedBudget+1);if(encoded.length>encodedBudget)throw limit("max-page-bytes");
                            data=URLDecoder.decode(new String(encoded,StandardCharsets.UTF_8),StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
                        }finally{inflater.end();}
                    }
                    if(data.length>settings.pageBytes())throw limit("max-page-bytes");sourceBytes=data.length;model=xml(data).getDocumentElement();
                }
                cells+=model.getElementsByTagName("mxCell").getLength();if(cells>settings.totalCells())throw limit("max-total-cells");
                String cleaned=clean(model);expanded+=Math.max(sourceBytes,cleaned.getBytes(StandardCharsets.UTF_8).length);if(expanded>settings.totalBytes())throw limit("max-total-bytes");
                String name=diagram.getAttribute("name");if(name.length()>255)name=name.substring(0,255);if(name.isBlank())name=Integer.toString(pages.size()+1);
                pages.add(new Page(name,cleaned));
            }
            if(pages.isEmpty())throw invalid();return new Preview(filename,nodeName,List.copyOf(pages),settings.pageBytes());
        }catch(Problem problem){throw problem;}catch(Exception malformed){throw invalid();}
    }
    private Document xml(byte[] data)throws Exception{
        // Count before constructing a DOM, including unknown tags that will later be discarded.
        var streamFactory=javax.xml.stream.XMLInputFactory.newFactory();
        streamFactory.setProperty(javax.xml.stream.XMLInputFactory.SUPPORT_DTD,false);
        streamFactory.setProperty("javax.xml.stream.isSupportingExternalEntities",false);
        streamFactory.setXMLResolver((publicId,systemId,base,namespace)->{throw new javax.xml.stream.XMLStreamException("External resource denied");});
        var scanner=streamFactory.createXMLStreamReader(new ByteArrayInputStream(data));
        try{int count=0,depth=0;while(scanner.hasNext()){
            int event=scanner.next();
            if(event==javax.xml.stream.XMLStreamConstants.DTD||event==javax.xml.stream.XMLStreamConstants.ENTITY_REFERENCE)throw invalid();
            if(event==javax.xml.stream.XMLStreamConstants.START_ELEMENT){if(++count>settings.xmlElements())throw limit("max-xml-elements");if(++depth>settings.xmlDepth())throw limit("max-xml-depth");if(scanner.getAttributeCount()>settings.xmlAttributes())throw limit("max-xml-attributes");}
            else if(event==javax.xml.stream.XMLStreamConstants.END_ELEMENT)depth--;
        }}finally{scanner.close();}

        var factory=DocumentBuilderFactory.newInstance();factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth",settings.xmlDepth());
        factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/elementAttributeLimit",settings.xmlAttributes());
        var builder=factory.newDocumentBuilder();builder.setErrorHandler(new DefaultHandler());return builder.parse(new ByteArrayInputStream(data));
    }
    private String plain(String value,boolean htmlLabel){
        if(value.length()>settings.labelCharacters())throw limit("max-label-characters");
        if(!htmlLabel)return org.jsoup.nodes.Entities.escape(value).replace("\r\n","\n").replace("\r","\n").replace("\n","<br>");
        var html=org.jsoup.Jsoup.parse(value);html.outputSettings().prettyPrint(false);
        html.select("script,style,iframe,object,embed,svg,math").remove();
        html.select("img").forEach(image->image.replaceWith(new org.jsoup.nodes.TextNode("[Image: "+imageName(image.attr("src"))+"]")));
        var safe=new org.jsoup.safety.Safelist().addTags("div","p","span","b","strong","i","em","u","s","strike","sub","sup","br","ul","ol","li","pre","code","font").addAttributes(":all","style","align").addAttributes("font","color","size","face");
        html=new org.jsoup.safety.Cleaner(safe).clean(html);html.outputSettings().prettyPrint(false);
        if(html.getAllElements().size()>settings.xmlElements())throw limit("max-xml-elements");
        for(var element:html.getAllElements()){
            if(element.parents().size()>settings.xmlDepth())throw limit("max-xml-depth");
            StringBuilder css=new StringBuilder();
            for(String entry:element.attr("style").split(";")){
                int colon=entry.indexOf(':');if(colon<0)continue;String key=entry.substring(0,colon).strip().toLowerCase(Locale.ROOT),v=entry.substring(colon+1).strip();
                if(!Set.of("text-align","vertical-align","white-space","font-size","font-family","font-weight","font-style","color","background-color","text-decoration","line-height","margin","padding","word-wrap","overflow-wrap").contains(key)||v.length()>200||!v.matches("[A-Za-z0-9_#.,% +\\-]+"))continue;
                var numbers=java.util.regex.Pattern.compile("[0-9]+(?:\\.[0-9]+)?").matcher(v);boolean large=false;while(numbers.find())if(Double.parseDouble(numbers.group())>4096)large=true;
                if(!large)css.append(key).append(':').append(v).append(';');
            }
            if(element.hasAttr("align")&&!Set.of("left","right","center","justify").contains(element.attr("align")))element.removeAttr("align");
            element.removeAttr("style");if(!css.isEmpty())element.attr("style",css.toString());
            for(String attr:List.of("color","size","face"))if(element.hasAttr(attr)&&!element.attr(attr).matches("[A-Za-z0-9_#., +\\-]{1,100}"))element.removeAttr(attr);
        }
        return html.body().html();
    }
    private static String imageName(String value){
        if(value.startsWith("data:"))return "embedded image";
        String name=value.split("[?#]",2)[0].replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);if(name.isBlank())name="image";return name.length()>160?name.substring(0,160):name;
    }
    private String clean(Element model)throws Exception{
        if(!"mxGraphModel".equals(model.getTagName()))throw invalid();
        var source=model.getElementsByTagName("mxCell");if(source.getLength()>settings.cells())throw limit("max-cells-per-page");
        var result=xml("<mxGraphModel><root/></mxGraphModel>".getBytes(StandardCharsets.UTF_8));var root=(Element)result.getDocumentElement().getFirstChild();
        var parents=new HashMap<String,String>();var cells=new HashMap<String,Element>();
        for(int i=0;i<source.getLength();i++){
            var cell=(Element)source.item(i);var wrapper=(Element)cell.getParentNode();String id=cell.getAttribute("id");if(id.isEmpty())id=wrapper.getAttribute("id");
            if(!id.matches("[A-Za-z0-9_.:-]{1,160}")||cells.containsKey(id))throw invalid();
            var copy=result.createElement("mxCell");copy.setAttribute("id",id);parents.put(id,cell.getAttribute("parent"));cells.put(id,copy);
            for(String key:List.of("parent","source","target","vertex","edge","connectable","visible","collapsed"))if(cell.hasAttribute(key))copy.setAttribute(key,cell.getAttribute(key));
            String value=cell.hasAttribute("value")?cell.getAttribute("value"):wrapper.getAttribute("label");value=plain(value,Arrays.asList(cell.getAttribute("style").split(";")).contains("html=1"));
            var style=new StringBuilder();String image=null;
            for(String token:cell.getAttribute("style").split(";")){
                int equals=token.indexOf('=');String key=equals<0?token:token.substring(0,equals),v=equals<0?"":token.substring(equals+1);
                if(key.equalsIgnoreCase("image")){image=imageName(v);continue;}
                if(Set.of("html","imageBackground","imageBorder","fontSource","link","whiteSpace","flowAnimation","animated","sketch","jiggle").contains(key))continue;
                if(v.equalsIgnoreCase("NaN")||v.equalsIgnoreCase("Infinity"))continue;
                if(v.matches("[+\\-]?[0-9.]+([eE][+\\-]?[0-9]+)?")){try{double number=Double.parseDouble(v);if(!Double.isFinite(number)||Math.abs(number)>10000)continue;}catch(NumberFormatException invalid){continue;}}
                if(key.matches("[A-Za-z][A-Za-z0-9_]{0,60}")&&v.length()<=200&&v.matches("[A-Za-z0-9_#., +\\-]*"))style.append(key).append(equals<0?"":"="+v).append(';');
            }
            if(image!=null){value+=(value.isBlank()?"":"<br>")+org.jsoup.nodes.Entities.escape("[Image: "+image+"]");style.append("shape=rectangle;");}
            copy.setAttribute("value",value);copy.setAttribute("style",style+"html=1;whiteSpace=wrap;");
            for(Node node=cell.getFirstChild();node!=null;node=node.getNextSibling())if(node instanceof Element e&&"mxGeometry".equals(e.getTagName()))copy.appendChild(geometry(result,e,0));
            root.appendChild(copy);
        }
        if(!cells.containsKey("0")||!cells.containsKey("1"))throw invalid();
        var checked=new HashSet<String>();
        for(String id:cells.keySet()){
            var path=new HashSet<String>();String at=id;
            while(!at.isEmpty()&&!checked.contains(at)){if(!parents.containsKey(at)||!path.add(at))throw invalid();if(path.size()>settings.parentDepth())throw limit("max-parent-depth");at=parents.get(at);}checked.addAll(path);
        }
        for(var cell:cells.values())for(String ref:List.of("source","target"))if(cell.hasAttribute(ref)&&(!cells.containsKey(cell.getAttribute(ref))||!"1".equals(cells.get(cell.getAttribute(ref)).getAttribute("vertex"))))cell.removeAttribute(ref);
        String output=serialize(result);if(output.getBytes(StandardCharsets.UTF_8).length>settings.pageBytes())throw limit("max-page-bytes");return output;
    }
    private static String serialize(Node node)throws Exception{
        var factory=TransformerFactory.newInstance();factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
        var transformer=factory.newTransformer();transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");var out=new StringWriter();transformer.transform(new DOMSource(node),new StreamResult(out));return out.toString();
    }
    private Element geometry(Document doc,Element source,int depth){
        if(depth>settings.geometryDepth())throw limit("max-geometry-depth");
        if(!Set.of("mxGeometry","mxPoint","Array","mxRectangle").contains(source.getTagName()))throw invalid();var target=doc.createElement(source.getTagName());
        for(String key:List.of("x","y","width","height","relative"))if(source.hasAttribute(key)){
            double value;try{value=Double.parseDouble(source.getAttribute(key));}catch(NumberFormatException e){throw invalid();}
            if(!Double.isFinite(value))throw invalid();if(Math.abs(value)>settings.coordinate())throw limit("max-coordinate");if(key.equals("relative")){if(value!=0&&value!=1)throw invalid();target.setAttribute(key,value==1?"1":"0");}else target.setAttribute(key,Double.toString(value));
        }
        if(source.hasAttribute("as")){String as=source.getAttribute("as");if(!Set.of("geometry","points","sourcePoint","targetPoint","offset","alternateBounds").contains(as))throw invalid();target.setAttribute("as",as);}
        int count=0;for(Node child=source.getFirstChild();child!=null;child=child.getNextSibling())if(child instanceof Element e){if(++count>settings.geometryPoints())throw limit("max-geometry-points");target.appendChild(geometry(doc,e,depth+1));}return target;
    }
}
