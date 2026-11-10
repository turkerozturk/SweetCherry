package com.turkerozturk.importing.zip;

import com.turkerozturk.helpers.*;
import com.turkerozturk.jdbc.JdbcIdentifiers;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/** A bounded snapshot, independent of the uploaded file. No ZIP member is extracted to a path. */
public final class ZipImportData {
    public static final int MAX_NODES=10000, MAX_DEPTH=64, TEXT_BYTES=2_000_000;
    public static final long MAX_EXPANDED=64_000_000, MAX_RETAINED=32_000_000, MAX_ARCHIVE=256_000_000;
    public record Options(boolean removeExtension,boolean attachments,boolean metadata,String charset) {
        public Options { if(!Set.of("UTF-8","windows-1254","windows-1252","ISO-8859-9").contains(charset))throw problem("encoding"); }
    }
    public enum Action { DIRECTORY, TEXT, ATTACHMENT, METADATA, SKIP }
    public static final class Entry {
        private final String path,name,syntax,text; private final int parent,icon; private final Action action;
        private final long size,created,modified; private final byte[] bytes;
        Entry(String path,String name,String syntax,String text,int parent,int icon,Action action,long size,long created,long modified,byte[] bytes) {
            this.path=path;this.name=name;this.syntax=syntax;this.text=text;this.parent=parent;this.icon=icon;this.action=action;
            this.size=size;this.created=created;this.modified=modified;this.bytes=bytes==null?null:bytes.clone();
        }
        public String path(){return path;} public String name(){return name;} public String syntax(){return syntax;}
        public String text(){return text;} public int parent(){return parent;} public int icon(){return icon;}
        public Action action(){return action;} public long size(){return size;}
        public long created(){return created;} public long modified(){return modified;}
    }
    private final List<Entry> entries; private final int attachmentLimit; private final long importedAt;
    private ZipImportData(List<Entry> entries,int attachmentLimit,long importedAt){this.entries=List.copyOf(entries);this.attachmentLimit=attachmentLimit;this.importedAt=importedAt;}
    public List<Entry> entries(){return entries;}
    public long count(){return entries.stream().filter(e->e.action!=Action.SKIP).count();}
    public long count(String action){return entries.stream().filter(e->e.action.name().equals(action)).count();}
    public int attachmentLimit(){return attachmentLimit;}
    public long attachmentBytes(){return entries.stream().filter(e->e.action==Action.ATTACHMENT).mapToLong(e->e.size).sum();}
    public static ZipImportData read(Path archive,Options options,int attachmentLimit)throws IOException {
        if(attachmentLimit<1||attachmentLimit>20_000_000)throw problem("limit");
        long now=Instant.now().getEpochSecond(), expanded=0,retained=0;
        var nodes=new LinkedHashMap<String,ZipEntry>();var explicit=new HashSet<String>();
        try(var zip=new ZipFile(archive.toFile(),StandardCharsets.UTF_8)) {
            if(zip.size()>MAX_NODES)throw problem("limit");
            var all=zip.entries();
            while(all.hasMoreElements()) {
                ZipEntry entry=all.nextElement();String path=path(entry.getName());
                if(!explicit.add(path))throw problem("path");
                String[] parts=path.split("/");String current="";
                for(int i=0;i<parts.length-1;i++) {
                    current=current.isEmpty()?parts[i]:current+"/"+parts[i];
                    var existing=nodes.get(current);if(existing!=null&&!existing.isDirectory())throw problem("path");
                    nodes.putIfAbsent(current,new ZipEntry(current+"/"));
                }
                var previous=nodes.get(path);
                if(previous!=null && (!previous.isDirectory()||!entry.isDirectory()))throw problem("path");
                nodes.put(path,entry);
                if(nodes.size()>MAX_NODES)throw problem("limit");
            }
            if(nodes.isEmpty())throw problem("empty");
            var entries=new ArrayList<Entry>();var indices=new HashMap<String,Integer>();
            for(var item:nodes.entrySet()) {
                String path=item.getKey();ZipEntry entry=item.getValue();int slash=path.lastIndexOf('/');
                int parent=slash<0?-1:indices.get(path.substring(0,slash));String original=path.substring(slash+1),name=original;
                long created=time(entry.getCreationTime()),modified=time(entry.getLastModifiedTime()),size=entry.getSize();
                Action action=Action.DIRECTORY;String text="",syntax="plain-text";int icon=NodeIcon.CT_DIRECTORY.getIconIdIn16bit();byte[] bytes=null;
                if(!entry.isDirectory()) {
                    if(size<0)throw problem("zip");
                    if(size>Math.max(TEXT_BYTES,attachmentLimit))action=options.metadata?Action.METADATA:Action.SKIP;
                    else {
                        if((expanded=Math.addExact(expanded,size))>MAX_EXPANDED)throw problem("limit");
                        try(var input=zip.getInputStream(entry)){bytes=input.readNBytes(Math.toIntExact(size)+1);}
                        if(bytes.length!=size)throw problem("zip");
                        var crc=new CRC32();crc.update(bytes);if(crc.getValue()!=entry.getCrc())throw problem("zip");
                        String decoded=decode(bytes,options.charset,original);
                        if(decoded!=null && bytes.length<=TEXT_BYTES) {action=Action.TEXT;text=decoded;}
                        else if(decoded==null && options.attachments && size<=attachmentLimit)action=Action.ATTACHMENT;
                        else if(decoded!=null || size>attachmentLimit)action=options.metadata?Action.METADATA:Action.SKIP;
                        else action=Action.SKIP;
                    }
                    if(action==Action.TEXT) {
                        int dot=original.lastIndexOf('.');CodeIcon mapping=CodeIcon.getCodeIconByFileExtension(dot>0?original.substring(dot+1):"");
                        syntax=mapping==CodeIcon.OTHER?"plain-text":mapping.getSyntax();icon=mapping==CodeIcon.OTHER?NodeIcon.CT_NOTES.getIconIdIn16bit():mapping.getNodeIcon().getIconIdIn16bit();
                    } else if(action==Action.ATTACHMENT) {
                        syntax="custom-colors";text="<node><rich_text></rich_text></node>";icon=NodeIcon.CT_FILE.getIconIdIn16bit();
                    } else if(action==Action.METADATA) {
                        text="File: "+original+"\nZIP path: "+path+"\nSize: "+size+" bytes\nCreated (UTC): "+date(created)+"\nModified (UTC): "+date(modified)+"\nAccessed (UTC): "+date(time(entry.getLastAccessTime()))+"\n\nContent omitted: file exceeds the text or attachment limit.";
                        icon=NodeIcon.CT_CANCEL.getIconIdIn16bit();
                    }
                    if(options.removeExtension && (action==Action.TEXT||action==Action.ATTACHMENT)) {
                        int dot=name.lastIndexOf('.');if(dot>0)name=name.substring(0,dot);
                    }
                    // Keep only the bytes that will actually be inserted as an attachment.
                    if(action!=Action.ATTACHMENT)bytes=null;
                    retained+=text.length()*2L+(bytes==null?0:bytes.length);if(retained>MAX_RETAINED)throw problem("limit");
                }
                indices.put(path,entries.size());entries.add(new Entry(path,name,syntax,text,parent,icon,action,size,created,modified,bytes));
            }
            return new ZipImportData(entries,attachmentLimit,now);
        }catch(ZipException e){throw problem("zip");}
    }
    static String path(String raw) {
        if(raw==null||raw.isEmpty()||raw.startsWith("/")||raw.startsWith("\\")||raw.indexOf('\\')>=0||raw.indexOf(':')>=0||raw.length()>4096||raw.codePoints().anyMatch(Character::isISOControl))throw problem("path");
        String path=raw.endsWith("/")?raw.substring(0,raw.length()-1):raw;String[] parts=path.split("/",-1);
        if(parts.length>MAX_DEPTH)throw problem("limit");
        for(String part:parts)if(part.isEmpty()||part.equals(".")||part.equals("..")||part.codePointCount(0,part.length())>255)throw problem("path");
        return path;
    }
    private static String decode(byte[] bytes,String charset,String filename) {
        String ext=filename.substring(filename.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        for(byte[] signature:List.of(new byte[]{(byte)0x89,80,78,71},new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff},new byte[]{80,75,3,4},new byte[]{0x1f,(byte)0x8b},new byte[]{0x7f,69,76,70},new byte[]{77,90},"%PDF-".getBytes(StandardCharsets.US_ASCII),"GIF8".getBytes(StandardCharsets.US_ASCII),"SQLite format 3".getBytes(StandardCharsets.US_ASCII)))
            if(bytes.length>=signature.length&&Arrays.equals(bytes,0,signature.length,signature,0,signature.length))return null;
        if(Set.of("pdf","png","jpg","jpeg","gif","webp","bmp","ico","zip","gz","7z","rar","ctb","sqlite","db","exe","dll","so","jar","class","doc","docx","xls","xlsx","ppt","pptx","mp3","mp4","ogg","woff","woff2","ttf").contains(ext))return null;
        int skip=0;Charset cs=Charset.forName(charset);
        if(bytes.length>=3&&bytes[0]==(byte)0xef&&bytes[1]==(byte)0xbb&&bytes[2]==(byte)0xbf){cs=StandardCharsets.UTF_8;skip=3;}
        else if(bytes.length>=2&&bytes[0]==(byte)0xff&&bytes[1]==(byte)0xfe){cs=StandardCharsets.UTF_16LE;skip=2;}
        else if(bytes.length>=2&&bytes[0]==(byte)0xfe&&bytes[1]==(byte)0xff){cs=StandardCharsets.UTF_16BE;skip=2;}
        try {
            String text=cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes,skip,bytes.length-skip)).toString();
            return text.codePoints().anyMatch(c->(Character.isISOControl(c)&&c!=9&&c!=10&&c!=13&&c!=12)||c==0xfffd)?null:text;
        }catch(CharacterCodingException e){return null;}
    }
    private static long time(java.nio.file.attribute.FileTime time){return time==null||time.toMillis()<0?-1:time.toInstant().getEpochSecond();}
    private static String date(long time){return time<0?"Not available in ZIP":DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(Instant.ofEpochSecond(time));}
    private static IllegalArgumentException problem(String key){return new IllegalArgumentException(key);}

    /** Caller owns the transaction; nothing can overwrite an existing node. */
    int insert(Connection c)throws SQLException {
        if(c.getAutoCommit()||c.isReadOnly()||!c.getMetaData().supportsTransactions())throw new SQLException("Writable transaction required");
        var required=new HashSet<>(Set.of("node","children"));if(count("ATTACHMENT")>0)required.add("image");
        String product=c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if(product.contains("mysql")||product.contains("mariadb")) {
            var transactional=new HashSet<String>();try(var s=c.createStatement();var r=s.executeQuery("SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()")){while(r.next())if("InnoDB".equalsIgnoreCase(r.getString(2)))transactional.add(r.getString(1));}
            if(!transactional.containsAll(required))throw new SQLException("Transactional tables required");
        }
        long max=scalar(c,"SELECT COALESCE(MAX(node_id),0) FROM (SELECT node_id FROM node UNION ALL SELECT node_id FROM children) AS zip_ids");
        Math.addExact(max,count());long root=scalar(c,"SELECT COALESCE(MAX(sequence),0) FROM children WHERE father_id=0");
        Math.addExact(root,entries.stream().filter(e->e.parent<0&&e.action!=Action.SKIP).count());
        String[] names={"node_id","name","txt","syntax","tags","is_ro","is_richtxt","has_codebox","has_table","has_image","level","ts_creation","ts_lastsave"};
        var columns=new ArrayList<String>();for(String name:names)columns.add(JdbcIdentifiers.quote(c,name));
        long[] ids=new long[entries.size()];var sequences=new HashMap<Integer,Integer>();
        try(var node=c.prepareStatement("INSERT INTO "+JdbcIdentifiers.quote(c,"node")+" ("+String.join(",",columns)+") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
            var child=c.prepareStatement("INSERT INTO "+JdbcIdentifiers.quote(c,"children")+" (node_id,father_id,sequence,master_id) VALUES (?,?,?,?)")) {
            for(int i=0;i<entries.size();i++) {
                Entry e=entries.get(i);if(e.action==Action.SKIP)continue;long id=++max;ids[i]=id;
                Object[] values={id,e.name,e.text,e.syntax,"",e.icon,e.action==Action.ATTACHMENT?1:0,0,0,e.action==Action.ATTACHMENT?1:0,0,e.created<0?importedAt:e.created,e.modified<0?importedAt:e.modified};
                for(int j=0;j<values.length;j++)node.setObject(j+1,values[j]);node.executeUpdate();
                child.setLong(1,id);child.setLong(2,e.parent<0?0:ids[e.parent]);child.setLong(3,e.parent<0?++root:sequences.merge(e.parent,1,Integer::sum));child.setLong(4,0);child.executeUpdate();
                if(e.action==Action.ATTACHMENT) {
                    try(var image=c.prepareStatement("INSERT INTO image (node_id,"+JdbcIdentifiers.quote(c,"offset")+",justification,anchor,png,filename,link,time) VALUES (?,0,'left','',?,?,'',?)")) {
                        image.setLong(1,id);image.setBytes(2,e.bytes);image.setString(3,e.path.substring(e.path.lastIndexOf('/')+1));image.setLong(4,e.modified<0?importedAt:e.modified);image.executeUpdate();
                    }
                }
            }
        }
        return Math.toIntExact(count());
    }
    private static long scalar(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
}
