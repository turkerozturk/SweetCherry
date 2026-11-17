package com.turkerozturk.image.attachments.preview;

import com.turkerozturk.jdbc.JdbcIdentifiers;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.sql.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

/** Bounded reading views only; archive entries are never extracted or opened. */
@Service @Transactional(readOnly=true)
public class FilePreviewService {
    @PersistenceContext private EntityManager manager;
    private final FilePreviewSettings settings;
    public FilePreviewService(FilePreviewSettings settings){this.settings=settings;}
    public static final List<String> ENCODINGS=List.of("UTF-8","UTF-16","UTF-16LE","UTF-16BE","windows-1254","ISO-8859-9","windows-1252","ISO-8859-1","US-ASCII");
    private static final Map<String,String> LANGUAGES=Map.ofEntries(
            Map.entry("java","java"),Map.entry("js","javascript"),Map.entry("mjs","javascript"),Map.entry("cjs","javascript"),
            Map.entry("ts","typescript"),Map.entry("json","json"),Map.entry("xml","xml"),Map.entry("html","xml"),Map.entry("htm","xml"),Map.entry("svg","xml"),
            Map.entry("css","css"),Map.entry("sql","sql"),Map.entry("py","python"),Map.entry("rb","ruby"),Map.entry("php","php"),
            Map.entry("c","c"),Map.entry("h","c"),Map.entry("cpp","cpp"),Map.entry("hpp","cpp"),Map.entry("cs","csharp"),Map.entry("go","go"),Map.entry("rs","rust"),
            Map.entry("kt","kotlin"),Map.entry("sh","bash"),Map.entry("bash","bash"),Map.entry("ps1","powershell"),Map.entry("bat","dos"),Map.entry("cmd","dos"),
            Map.entry("yml","yaml"),Map.entry("yaml","yaml"),Map.entry("md","markdown"),Map.entry("ini","ini"),Map.entry("toml","ini"),Map.entry("properties","properties"));
    public static String language(String filename){String n=Objects.requireNonNullElse(filename,"").toLowerCase(Locale.ROOT);return LANGUAGES.getOrDefault(n.substring(n.lastIndexOf('.')+1),"");}
    public static boolean archive(String filename){String n=Objects.requireNonNullElse(filename,"").toLowerCase(Locale.ROOT);return n.endsWith(".tar")||n.endsWith(".tgz")||n.endsWith(".tar.gz")||zipName(n);}
    private static boolean zipName(String n){return Set.of("zip","jar","war","ear","docx","xlsx","pptx","odt","ods","epub").contains(n.substring(n.lastIndexOf('.')+1));}
    private static String defaultMode(String filename){
        if(archive(filename))return "archive";
        String n=Objects.requireNonNullElse(filename,"").toLowerCase(Locale.ROOT),leaf=n.replace('\\','/');leaf=leaf.substring(leaf.lastIndexOf('/')+1);
        return !language(n).isEmpty()||Set.of("txt","text","log","csv","tsv","conf","cfg","env","gradle","tex","mm","srt","vtt").contains(n.substring(n.lastIndexOf('.')+1))
                ||Set.of("readme","license","licence","dockerfile","makefile",".gitignore",".env").contains(leaf)?"text":"hex";
    }
    public record Options(String mode,String encoding,long page){
        public Options{if(!Set.of("auto","text","hex","archive").contains(mode)||!ENCODINGS.contains(encoding)||page<1)throw new Problem("request");}
    }
    public static final class Problem extends RuntimeException {private final String code;public Problem(String code){super(code);this.code=code;}public String code(){return code;}}
    public record HexRow(String offset,String hex,String ascii) { }
    public record Entry(String name,String kind,long bytes,long compressed,String modified,String crc,String method,String comment,String target,boolean encrypted) { }
    public record Preview(String filename,String nodeName,long bytes,String mode,String encoding,String language,String text,
            List<HexRow> hex,List<Entry> entries,boolean clipped,boolean replaced,boolean binary,long page,long pages,long start,long shown) { }
    public Preview preview(long nodeId,int offset,Options options){
        if(nodeId<=0||offset<0)throw new Problem("request");
        return manager.unwrap(Session.class).doReturningWork(c->load(c,nodeId,offset,options));
    }
    Preview load(Connection c,long nodeId,int offset,Options options)throws SQLException{
        if(nodeId<=0||offset<0)throw new Problem("request");
        String length=c.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")?"OCTET_LENGTH(i.png)":"LENGTH(i.png)";
        String from=" FROM image i JOIN node n ON n.node_id=i.node_id WHERE i.node_id=? AND i."+JdbcIdentifiers.quote(c,"offset")+"=? AND i.png IS NOT NULL AND COALESCE(i.anchor,'')='' AND COALESCE(i.filename,'')<>'' AND i.filename<>'__ct_special.tex'";
        String filename,node;long bytes;
        try(var q=c.prepareStatement("SELECT i.filename,n.name,"+length+from)){
            q.setLong(1,nodeId);q.setInt(2,offset);try(var rs=q.executeQuery()){
                if(!rs.next())throw new Problem("missing");filename=rs.getString(1);node=rs.getString(2);bytes=rs.getLong(3);
            }
        }
        String mode=options.mode().equals("auto")?defaultMode(filename):options.mode();
        if(mode.equals("archive")&&!archive(filename))throw new Problem("unsupported");
        long start=0,pages=1;int limit=settings.textBytes();
        if(mode.equals("archive")){if(bytes>settings.archiveBytes())throw new Problem("large");limit=settings.archiveBytes();}
        if(mode.equals("hex")){
            pages=bytes==0?1:(bytes-1)/settings.hexBytes()+1;
            if(options.page()>pages)throw new Problem("request");
            start=(options.page()-1)*settings.hexBytes();limit=settings.hexBytes();
        }else if(options.page()!=1)throw new Problem("request");
        try(var q=c.prepareStatement("SELECT i.filename,"+length+",i.png"+from)){
            q.setLong(1,nodeId);q.setInt(2,offset);try(var rs=q.executeQuery()){
                if(!rs.next())throw new Problem("missing");
                if(!filename.equals(rs.getString(1))||bytes!=rs.getLong(2))throw new Problem("changed");
                try(var in=rs.getBinaryStream(3)){
                    if(in==null)throw new Problem("missing");in.skipNBytes(start);
                    byte[] data=in.readNBytes(limit+1);
                    if(mode.equals("archive")&&data.length>limit)throw new Problem("large");
                    // Detect concurrent shortening instead of presenting a misleading byte range.
                    if(data.length<Math.min((long)limit,bytes-start))throw new Problem("changed");
                    return parse(data,filename,node,bytes,mode,options.encoding(),options.page(),pages,start,limit);
                }catch(IOException invalid){throw new Problem("invalid");}
            }
        }
    }
    Preview parse(byte[] data,String filename,String node,long bytes,String mode,String encoding,long page,long pages,long start,int limit){
        boolean clipped=data.length>limit||mode.equals("text")&&bytes>limit;
        data=Arrays.copyOf(data,Math.min(data.length,limit));String text="",language="";boolean replaced=false,binary=false;
        var hex=new ArrayList<HexRow>();List<Entry> entries=List.of();
        if(mode.equals("text")){
            var decoder=Charset.forName(encoding).newDecoder();var input=ByteBuffer.wrap(data);var output=CharBuffer.allocate(Math.max(1,data.length+1));
            var result=decoder.decode(input,output,!clipped);replaced=result.isError();
            if(replaced){decoder.reset().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);input.rewind();output.clear();decoder.decode(input,output,!clipped);}
            output.flip();text=output.toString();if(text.startsWith("\ufeff"))text=text.substring(1);
            binary=text.codePoints().anyMatch(ch->Character.isISOControl(ch)&&ch!='\n'&&ch!='\r'&&ch!='\t');
            // Control bytes have no useful text representation; hex preserves their exact value.
            text=text.codePoints().map(ch->Character.isISOControl(ch)&&ch!='\n'&&ch!='\r'&&ch!='\t'?0xfffd:ch).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString();
            if(text.codePointCount(0,text.length())>settings.characters()){text=text.substring(0,text.offsetByCodePoints(0,settings.characters()));clipped=true;}
            language=language(filename);
        }else if(mode.equals("hex")){
            for(int i=0;i<data.length;i+=16){var h=new StringBuilder();var a=new StringBuilder();for(int j=i;j<Math.min(i+16,data.length);j++){int b=data[j]&255;if(j>i)h.append(' ');h.append(String.format(Locale.ROOT,"%02X",b));a.append(b>=32&&b<=126?(char)b:'.');}hex.add(new HexRow(String.format(Locale.ROOT,"%016X",start+i),h.toString(),a.toString()));}
            clipped=false;
        }else if(mode.equals("archive")){
            entries=archiveEntries(data,filename);clipped=entries.size()>settings.entries();if(clipped)entries=List.copyOf(entries.subList(0,settings.entries()));
        }else throw new Problem("request");
        return new Preview(filename,Objects.requireNonNullElse(node,""),bytes,mode,encoding,language,text,List.copyOf(hex),entries,clipped,replaced,binary,page,pages,start,data.length);
    }
    private List<Entry> archiveEntries(byte[] data,String filename){
        String name=filename.toLowerCase(Locale.ROOT);var rows=new ArrayList<Entry>();
        try{
            if(zipName(name)){
                // Metadata only: no entry streams are opened, including encrypted members.
                try(var channel=new SeekableInMemoryByteChannel(data);var zip=ZipFile.builder().setSeekableByteChannel(channel).setIgnoreLocalFileHeader(true).get()){
                    var items=zip.getEntries();while(items.hasMoreElements()&&rows.size()<=settings.entries()){
                        var e=items.nextElement();rows.add(new Entry(shortText(e.getName()),e.isDirectory()?"directory":e.isUnixSymlink()?"symlink":"file",e.getSize(),e.getCompressedSize(),e.getLastModifiedTime()==null?"":e.getLastModifiedTime().toInstant().toString(),e.getCrc()<0?"":String.format(Locale.ROOT,"%08X",e.getCrc()),Integer.toString(e.getMethod()),shortText(e.getComment()),"",e.getGeneralPurposeBit().usesEncryption()));
                    }
                }
            }else{
                if(name.endsWith(".gz")||name.endsWith(".tgz"))try(var gzip=new GZIPInputStream(new ByteArrayInputStream(data))){data=gzip.readNBytes(settings.expandedBytes()+1);if(data.length>settings.expandedBytes())throw new Problem("large");}
                if(data.length<512||!tarHeader(data))throw new Problem("invalid");
                try(var tar=new TarArchiveInputStream(new ByteArrayInputStream(data))){
                    while(rows.size()<=settings.entries()){
                        var e=tar.getNextEntry();if(e==null)break;if(!e.isCheckSumOK())throw new Problem("invalid");
                        rows.add(new Entry(shortText(e.getName()),e.isDirectory()?"directory":e.isSymbolicLink()?"symlink":e.isLink()?"hardlink":e.isFile()?"file":"other",e.getSize(),-1,e.getLastModifiedDate().toInstant().toString(),"","", "",shortText(e.getLinkName()),false));
                    }
                }
            }
        }catch(Problem error){throw error;}catch(IOException|RuntimeException error){throw new Problem("invalid");}
        return List.copyOf(rows);
    }
    private static boolean tarHeader(byte[] data){ // Accept a valid empty TAR, but never an arbitrary renamed binary.
        boolean zero=true;for(int i=0;i<512;i++)if(data[i]!=0){zero=false;break;}
        return zero||org.apache.commons.compress.archivers.tar.TarArchiveInputStream.matches(data,data.length);
    }
    private static String shortText(String value){if(value==null)return "";return value.codePointCount(0,value.length())>512?value.substring(0,value.offsetByCodePoints(0,512))+"…":value;}
}
