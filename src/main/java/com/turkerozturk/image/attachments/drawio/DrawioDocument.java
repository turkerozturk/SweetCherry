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
    private int page;private String stage="xml";
    private DrawioDocument(DrawioSettings settings){this.settings=settings;}
    private static Problem limit(String key){return new Problem("large",key);}
    private static Problem invalid(){return new Problem("invalid");}
    private static Problem invalid(String reason){return new Problem("invalid").context(reason,0);}
    static Preview parse(byte[] bytes,String filename,String nodeName){return parse(bytes,filename,nodeName,DrawioSettings.defaults());}
    static Preview parse(byte[] bytes,String filename,String nodeName,DrawioSettings settings){return new DrawioDocument(settings).document(bytes,filename,nodeName);}
    private Preview document(byte[] bytes,String filename,String nodeName){
        if(!supports(filename))throw new Problem("unsupported");if(bytes.length>settings.fileBytes())throw limit("max-file-bytes");
        try{
            Document doc=xml(bytes);Element outer=doc.getDocumentElement();var pages=new ArrayList<Page>();long expanded=0,cells=0;
            if("mxGraphModel".equals(outer.getTagName())){page=1;stage="model";if(bytes.length>settings.pageBytes())throw limit("max-page-bytes");if(outer.getElementsByTagName("mxCell").getLength()>settings.totalCells())throw limit("max-total-cells");String model=clean(outer);if(Math.max(bytes.length,model.getBytes(StandardCharsets.UTF_8).length)>settings.totalBytes())throw limit("max-total-bytes");return new Preview(filename,nodeName,List.of(new Page("1",model)),settings.pageBytes());}
            if(!"mxfile".equals(outer.getTagName()))throw invalid("container");
            for(Node child=outer.getFirstChild();child!=null;child=child.getNextSibling()){
                if(!(child instanceof Element diagram)||!"diagram".equals(diagram.getTagName()))continue;
                page=pages.size()+1;stage="page_structure";
                if(pages.size()>=settings.pages())throw limit("max-pages");
                int sourceBytes=0;Element model=null;for(Node n=diagram.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element e){if(model!=null||!"mxGraphModel".equals(e.getTagName()))throw invalid();model=e;}
                if(model!=null){sourceBytes=serialize(model).getBytes(StandardCharsets.UTF_8).length;if(sourceBytes>settings.pageBytes())throw limit("max-page-bytes");}
                if(model==null){stage="compression";String text=diagram.getTextContent().strip();byte[] data;
                    if(text.isEmpty()){data=EMPTY_MODEL.getBytes(StandardCharsets.UTF_8);}
                    else if(text.startsWith("<"))data=text.getBytes(StandardCharsets.UTF_8);
                    else{byte[] compressed=Base64.getDecoder().decode(text.replaceAll("\\s+",""));var inflater=new Inflater(true);
                        try(var in=new InflaterInputStream(new ByteArrayInputStream(compressed),inflater)){
                            int encodedBudget=settings.pageBytes()*3;byte[] encoded=in.readNBytes(encodedBudget+1);if(encoded.length>encodedBudget)throw limit("max-page-bytes");
                            data=URLDecoder.decode(new String(encoded,StandardCharsets.UTF_8),StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
                        }finally{inflater.end();}
                    }
                    if(data.length>settings.pageBytes())throw limit("max-page-bytes");sourceBytes=data.length;stage="xml";model=xml(data).getDocumentElement();
                }
                stage="model";cells+=model.getElementsByTagName("mxCell").getLength();if(cells>settings.totalCells())throw limit("max-total-cells");
                String cleaned=clean(model);expanded+=Math.max(sourceBytes,cleaned.getBytes(StandardCharsets.UTF_8).length);if(expanded>settings.totalBytes())throw limit("max-total-bytes");
                String name=diagram.getAttribute("name");if(name.length()>255)name=name.substring(0,255);if(name.isBlank())name=Integer.toString(pages.size()+1);
                pages.add(new Page(name,cleaned,DrawioLinks.pageId(diagram.getAttribute("id"))));
            }
            if(pages.isEmpty())throw invalid("no_pages");return new Preview(filename,nodeName,List.copyOf(pages),settings.pageBytes());
        }catch(Problem problem){throw problem.context(stage,page);}catch(Exception malformed){
            var problem=invalid(stage).context(stage,page);
            if(malformed instanceof javax.xml.stream.XMLStreamException stream&&stream.getLocation()!=null)problem.location(stream.getLocation().getLineNumber(),stream.getLocation().getColumnNumber());
            if(malformed instanceof org.xml.sax.SAXParseException sax)problem.location(sax.getLineNumber(),sax.getColumnNumber());
            throw problem;
        }
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
            if(event==javax.xml.stream.XMLStreamConstants.DTD||event==javax.xml.stream.XMLStreamConstants.ENTITY_REFERENCE)throw invalid("unsafe_xml");
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
        html.select("a").forEach(anchor->{String link=DrawioLinks.safe(anchor.attr("href"));if(link==null)anchor.unwrap();else anchor.attr("href",link).attr("target","_blank").attr("rel","noopener noreferrer");});
        html.getAllElements().forEach(e->{e.removeAttr("data-sc-placeholder");e.removeAttr("data-sc-image-key");});
        html.select("img").forEach(image->{
            String name=imageName(image.attr("src")),alt=image.attr("alt").strip();
            if((name.equals("image")||name.equals("missing source")||name.startsWith("embedded"))&&!alt.isBlank())name=alt.substring(0,Math.min(160,alt.length()));
            image.replaceWith(new org.jsoup.nodes.Element("span").attr("data-sc-placeholder","1").attr("data-sc-image-key",imageKey(image.attr("src"))).text("↗ [Image: "+name+"]"));
        });
        var safe=new org.jsoup.safety.Safelist().addTags("a","div","p","span","b","strong","i","em","u","s","strike","sub","sup","br","ul","ol","li","pre","code","font").addAttributes(":all","style","align").addAttributes("a","href","target","rel").addProtocols("a","href","http","https","data").addAttributes("font","color","size","face").addAttributes("span","data-sc-placeholder","data-sc-image-key");
        html=new org.jsoup.safety.Cleaner(safe).clean(html);html.outputSettings().prettyPrint(false);
        if(html.getAllElements().size()>settings.xmlElements())throw limit("max-xml-elements");
        for(var element:html.getAllElements()){
            if(element.parents().size()>settings.xmlDepth())throw limit("max-xml-depth");
            StringBuilder css=new StringBuilder();
            for(String entry:element.attr("style").split(";")){
                int colon=entry.indexOf(':');if(colon<0)continue;String key=entry.substring(0,colon).strip().toLowerCase(Locale.ROOT),v=entry.substring(colon+1).strip();
                if(!Set.of("text-align","vertical-align","white-space","font-size","font-family","font-weight","font-style","color","background-color","background","text-decoration","line-height","margin","padding","word-wrap","overflow-wrap").contains(key)||v.length()>200)continue;
                if(Set.of("color","background-color","background").contains(key)){if(safeColor(v))css.append(key.equals("background")?"background-color":key).append(':').append(v).append(';');continue;}
                if(!v.matches("[A-Za-z0-9_#.,% +\\-]+"))continue;
                var numbers=java.util.regex.Pattern.compile("[0-9]+(?:\\.[0-9]+)?").matcher(v);boolean large=false;while(numbers.find())if(Double.parseDouble(numbers.group())>4096)large=true;
                if(!large)css.append(key).append(':').append(v).append(';');
            }
            if(element.hasAttr("data-sc-placeholder"))element.attr("class","sc-drawio-placeholder");
            if(element.hasAttr("align")&&!Set.of("left","right","center","justify").contains(element.attr("align")))element.removeAttr("align");
            element.removeAttr("style");if(!css.isEmpty())element.attr("style",css.toString());
            for(String attr:List.of("color","size","face"))if(element.hasAttr(attr)&&(attr.equals("color")?!safeColor(element.attr(attr)):!element.attr(attr).matches("[A-Za-z0-9_#., +\\-]{1,100}")))element.removeAttr(attr);
        }
        return html.body().html();
    }
    private static boolean safeColor(String value){
        return value.matches("#[0-9a-fA-F]{3}|#[0-9a-fA-F]{4}|#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8}|[A-Za-z]{1,30}")
            ||value.length()<=100&&value.matches("(?i)(?:rgb|rgba|hsl|hsla)\\([0-9.,% +\\-]+\\)");
    }
    static String imageKey(String value){
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static String placeholder(String label,String key){return "<span class=\"sc-drawio-placeholder\" data-sc-placeholder=\"1\" title=\"Local image key: image."+key+"\">"+org.jsoup.nodes.Entities.escape(label)+"</span>";}
    private static String imageName(String value){
        if(value.startsWith("data:image/svg"))return "embedded SVG image";
        if(value.startsWith("data:"))return "embedded image";
        String name=value.split("[?#]",2)[0].replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);if(name.isBlank())name="missing source";return name.length()>160?name.substring(0,160):name;
    }
    private static final String EMPTY_MODEL="<mxGraphModel><root><mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/></root></mxGraphModel>";
    private String clean(Element model)throws Exception{
        if(!"mxGraphModel".equals(model.getTagName()))throw invalid("model");
        var source=model.getElementsByTagName("mxCell");if(source.getLength()>settings.cells())throw limit("max-cells-per-page");
        var result=xml("<mxGraphModel><root/></mxGraphModel>".getBytes(StandardCharsets.UTF_8));var root=(Element)result.getDocumentElement().getFirstChild();
        if(source.getLength()==0){if(EMPTY_MODEL.length()>settings.pageBytes())throw limit("max-page-bytes");return EMPTY_MODEL;}
        var originals=new LinkedHashMap<String,Element>();var parents=new HashMap<String,String>();
        for(int i=0;i<source.getLength();i++){
            var cell=(Element)source.item(i);var wrapper=(Element)cell.getParentNode();
            String id=!"root".equals(wrapper.getTagName())&&wrapper.hasAttribute("id")?wrapper.getAttribute("id"):cell.getAttribute("id");
            if(id.isEmpty())throw invalid("cell_id");if(originals.putIfAbsent(id,cell)!=null)throw invalid("duplicate_cell");parents.put(id,cell.getAttribute("parent"));
        }
        var roots=originals.keySet().stream().filter(id->parents.get(id).isEmpty()).toList();
        if(roots.size()!=1)throw invalid("graph_root");String rootId=roots.get(0);var rootCell=originals.get(rootId);
        if("1".equals(rootCell.getAttribute("vertex"))||"1".equals(rootCell.getAttribute("edge")))throw invalid("graph_root");
        var layers=originals.keySet().stream().filter(id->rootId.equals(parents.get(id))&&!"1".equals(originals.get(id).getAttribute("vertex"))&&!"1".equals(originals.get(id).getAttribute("edge"))).toList();
        // Draw.io roots/layers need not use literal 0/1. Use safe lookup keys in mxCodec.
        var ids=new HashMap<String,String>();ids.put(rootId,"0");if(!layers.isEmpty())ids.put(layers.get(0),"1");
        int nextId=2;for(String id:originals.keySet())if(!ids.containsKey(id))ids.put(id,"sc"+nextId++);
        var cells=new HashMap<String,Element>();
        for(var entry:originals.entrySet()){
            String id=entry.getKey();var cell=entry.getValue();var wrapper=(Element)cell.getParentNode();
            String link=DrawioLinks.safe(wrapper.hasAttribute("link")?wrapper.getAttribute("link"):cell.getAttribute("link"));
            if(link==null)for(String token:cell.getAttribute("style").split(";"))if(token.startsWith("link=")){link=DrawioLinks.safe(token.substring(5));break;}
            var copy=result.createElement("mxCell");if(link!=null)copy.setAttribute("scLink",link);copy.setAttribute("id",ids.get(id));cells.put(id,copy);
            for(String key:List.of("vertex","edge","connectable","visible","collapsed"))if(cell.hasAttribute(key))copy.setAttribute(key,cell.getAttribute(key));
            String parent=parents.get(id);if(!parent.isEmpty()){if(!ids.containsKey(parent))throw invalid("missing_parent");copy.setAttribute("parent",ids.get(parent));}
            for(String ref:List.of("source","target")){String target=cell.getAttribute(ref);if(originals.containsKey(target)&&"1".equals(originals.get(target).getAttribute("vertex")))copy.setAttribute(ref,ids.get(target));}
            String value=!"root".equals(wrapper.getTagName())&&wrapper.hasAttribute("label")?wrapper.getAttribute("label"):cell.getAttribute("value");value=plain(value,Arrays.asList(cell.getAttribute("style").split(";")).contains("html=1"));
            var style=new StringBuilder();String image=null,missingKey=null;boolean hasImage=false;
            for(String token:cell.getAttribute("style").split(";")){
                int equals=token.indexOf('=');String key=equals<0?token:token.substring(0,equals),v=equals<0?"":token.substring(equals+1);
                if(key.equals("sourcePort")||key.equals("targetPort")){if(ids.containsKey(v))style.append(key).append('=').append(ids.get(v)).append(';');continue;}
                if(Set.of("fontColor","labelBackgroundColor","labelBorderColor").contains(key)){if(safeColor(v))style.append(key).append('=').append(v).append(';');continue;}
                if(key.equalsIgnoreCase("image")){hasImage=true;missingKey=imageKey(v);style.append("scImageKey=").append(missingKey).append(';');String icon=DrawioLibraries.iconPath(v);if(icon!=null)style.append("image=sc-icon:").append(icon).append(';');else{image=imageName(v);}continue;}
                if(Set.of("html","imageBackground","imageBorder","fontSource","link","whiteSpace","flowAnimation","animated","sketch","jiggle").contains(key))continue;
                if(v.equalsIgnoreCase("NaN")||v.equalsIgnoreCase("Infinity"))continue;
                if(v.matches("[+\\-]?[0-9.]+([eE][+\\-]?[0-9]+)?")){try{double number=Double.parseDouble(v);if(!Double.isFinite(number)||Math.abs(number)>10000)continue;}catch(NumberFormatException invalid){continue;}}
                if(key.matches("[A-Za-z][A-Za-z0-9_]{0,60}")&&v.length()<=200&&v.matches("[A-Za-z0-9_#., +\\-]*"))style.append(key).append(equals<0?"":"="+v).append(';');
            }
            if(image==null&&!hasImage&&Arrays.asList(cell.getAttribute("style").split(";")).contains("shape=image")){image="missing source";missingKey=imageKey("");style.append("scImageKey=").append(missingKey).append(';');}
            if(image!=null){
                var original=java.util.regex.Pattern.compile("(?:^|;)shape=([A-Za-z][A-Za-z0-9_.-]{0,199})(?:;|$)").matcher(cell.getAttribute("style"));
                if(original.find())style.append("scOriginalShape=").append(original.group(1)).append(';');
                if(image.equals("image")||image.startsWith("embedded")){
                    var shape=java.util.regex.Pattern.compile("(?:^|;)shape=([^;]+)").matcher(cell.getAttribute("style"));String name=shape.find()?shape.group(1):"";
                    if(!Set.of("image","rectangle","label","").contains(name))image=name;
                    else{String caption=org.jsoup.Jsoup.parse(value).text();if(!caption.isBlank())image=caption.substring(0,Math.min(100,caption.length()));}
                }
                value+=(value.isBlank()?"":"<br>")+placeholder("↗ [Image: "+image+"]",missingKey);style.append("shape=rectangle;");
            }
            copy.setAttribute("value",value);copy.setAttribute("style",style+"html=1;whiteSpace=wrap;");
            for(Node node=cell.getFirstChild();node!=null;node=node.getNextSibling())if(node instanceof Element e&&"mxGeometry".equals(e.getTagName()))copy.appendChild(geometry(result,e,0));
            root.appendChild(copy);
        }
        if(layers.isEmpty()){var layer=result.createElement("mxCell");layer.setAttribute("id","1");layer.setAttribute("parent","0");root.insertBefore(layer,cells.get(rootId).getNextSibling());}
        var checked=new HashSet<String>();
        for(String id:cells.keySet()){
            var path=new HashSet<String>();String at=id;
            while(!at.isEmpty()&&!checked.contains(at)){if(!parents.containsKey(at))throw invalid("missing_parent");if(!path.add(at))throw invalid("parent_cycle");if(path.size()>settings.parentDepth())throw limit("max-parent-depth");at=parents.get(at);}checked.addAll(path);
        }
        String output=serialize(result);if(output.getBytes(StandardCharsets.UTF_8).length>settings.pageBytes())throw limit("max-page-bytes");return output;
    }
    private static String serialize(Node node)throws Exception{
        var factory=TransformerFactory.newInstance();factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
        var transformer=factory.newTransformer();transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");var out=new StringWriter();transformer.transform(new DOMSource(node),new StreamResult(out));return out.toString();
    }
    private Element geometry(Document doc,Element source,int depth){
        if(depth>settings.geometryDepth())throw limit("max-geometry-depth");
        if(!Set.of("mxGeometry","mxPoint","Array","mxRectangle").contains(source.getTagName()))throw invalid("geometry_type");var target=doc.createElement(source.getTagName());
        for(String key:List.of("x","y","width","height","relative"))if(source.hasAttribute(key)){
            double value;try{value=Double.parseDouble(source.getAttribute(key));}catch(NumberFormatException e){throw invalid("geometry_number");}
            if(!Double.isFinite(value))throw invalid("geometry_number");if(Math.abs(value)>settings.coordinate())throw limit("max-coordinate");if(key.equals("relative")){if(value!=0&&value!=1)throw invalid("geometry_number");target.setAttribute(key,value==1?"1":"0");}else target.setAttribute(key,Double.toString(value));
        }
        if(source.hasAttribute("as")){String as=source.getAttribute("as");if(!Set.of("geometry","points","sourcePoint","targetPoint","offset","alternateBounds").contains(as))throw invalid("geometry_type");target.setAttribute("as",as);}
        int count=0;for(Node child=source.getFirstChild();child!=null;child=child.getNextSibling())if(child instanceof Element e){if(++count>settings.geometryPoints())throw limit("max-geometry-points");target.appendChild(geometry(doc,e,depth+1));}return target;
    }
}
