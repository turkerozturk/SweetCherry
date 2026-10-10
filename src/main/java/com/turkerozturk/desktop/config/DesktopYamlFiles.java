package com.turkerozturk.desktop.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.util.*;

/** Local desktop-only editor. No HTTP endpoint, Spring bean, or caller-supplied file path. */
public final class DesktopYamlFiles {
    private static final int MAX_BYTES=1024*1024;
    private static final List<String> DEFAULTS=List.of("application.yml","application-operations.yml","application-experiments.yml");
    public record Choice(String relative, boolean external) { @Override public String toString(){return relative;} }
    public record Snapshot(Choice choice,String yaml,String revision,boolean external) {}
    public record Field(String id,String key,String type,String value,boolean secret) {}
    private record Leaf(String id,String key,ScalarNode node) {}
    private final Path root;
    private final ClassLoader loader;
    public DesktopYamlFiles(){this(Path.of("."),DesktopYamlFiles.class.getClassLoader());}
    public DesktopYamlFiles(Path root,ClassLoader loader){this.root=root.toAbsolutePath().normalize();this.loader=loader;}
    public List<Choice> choices()throws IOException {
        TreeMap<String,Choice> result=new TreeMap<>();
        for(String name:DEFAULTS)if(loader.getResource(name)!=null)result.put(name,new Choice(name,Files.isRegularFile(root.resolve(name))));
        for(Path directory:List.of(root,root.resolve("config"))) {
            if(!Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS))continue;
            try(var files=Files.newDirectoryStream(directory)){
                for(Path file:files){String name=file.getFileName().toString().toLowerCase(Locale.ROOT);
                    if((name.endsWith(".yml")||name.endsWith(".yaml"))&&Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)){
                        String relative=root.relativize(file).toString().replace('\\','/');result.put(relative,new Choice(relative,true));
                        if(result.size()>256)throw new IOException("Too many YAML files");
                    }
                }
            }
        }
        var choices=new ArrayList<>(result.values());choices.sort(Comparator.comparingInt((Choice c)->c.relative().equals("application.yml")?0:c.relative().equals("application.yaml")?1:2).thenComparing(Choice::relative));
        return List.copyOf(choices);
    }
    private Path resolve(Choice choice)throws IOException {
        Path path=root.resolve(choice.relative()).normalize();
        if(!path.startsWith(root)||!(path.getParent().equals(root)||path.getParent().equals(root.resolve("config")))||!choice.relative().matches("(?i)(?:config/)?[^/\\\\]+\\.ya?ml"))throw new IOException("Unsupported configuration path");
        if(Files.isSymbolicLink(path)||Files.isSymbolicLink(path.getParent()))throw new IOException("Symbolic links cannot be edited");
        return path;
    }
    public Snapshot read(Choice choice)throws IOException {
        if(choices().stream().noneMatch(c->c.relative().equals(choice.relative())))throw new IOException("Configuration no longer exists");
        Path file=resolve(choice);boolean external=Files.exists(file);
        byte[] bytes;
        if(external){bytes=readBytes(file);}
        else{try(InputStream stream=loader.getResourceAsStream(choice.relative())){if(stream==null)throw new IOException("Default not found");bytes=stream.readNBytes(MAX_BYTES+1);}}
        if(bytes.length>MAX_BYTES)throw new IOException("YAML file exceeds 1 MB");
        String yaml=new String(bytes,StandardCharsets.UTF_8);
        // Invalid startup YAML can still be corrected in the raw tab; save always validates.
        return new Snapshot(choice,yaml,hash(bytes),external);
    }
    public synchronized boolean save(Snapshot snapshot,String yaml)throws IOException {
        validate(yaml);
        Path file=resolve(snapshot.choice());boolean exists=Files.exists(file);
        if(exists!=snapshot.external()||(exists&&!hash(readBytes(file)).equals(snapshot.revision())))throw new IOException("File changed; reload before saving");
        if(yaml.equals(snapshot.yaml()))return false; // Do not create an unchanged default file.
        byte[] bytes=yaml.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException("YAML file exceeds 1 MB");
        Path temporary=Files.createTempFile(file.getParent(),".sweetcherry-yaml-",".tmp");
        try {
            if(exists){
                if(Files.getFileStore(file).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(temporary,Files.getPosixFilePermissions(file));
                var acl=Files.getFileAttributeView(file,AclFileAttributeView.class);var target=Files.getFileAttributeView(temporary,AclFileAttributeView.class);
                if(acl!=null&&target!=null)target.setAcl(acl.getAcl());
            }
            Files.write(temporary,bytes);
            if(exists){if(!hash(readBytes(file)).equals(snapshot.revision()))throw new IOException("File changed; reload before saving");Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            else { // CREATE_NEW prevents silently replacing a file created by another editor.
                try(var out=Files.newByteChannel(file,Set.of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE),
                        Files.getFileStore(file.getParent()).supportsFileAttributeView("posix")?new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))}:new FileAttribute<?>[0])){var buffer=java.nio.ByteBuffer.wrap(bytes);while(buffer.hasRemaining())out.write(buffer);}
            }
        } finally {Files.deleteIfExists(temporary);}
        return true;
    }
    public static List<Field> fields(String yaml){
        List<Field> result=new ArrayList<>();for(var leaf:leaves(yaml)){ScalarNode node=leaf.node();String type=Tag.BOOL.equals(node.getTag())?"boolean":Tag.INT.equals(node.getTag())||Tag.FLOAT.equals(node.getTag())?"number":Tag.NULL.equals(node.getTag())?"null":"string";
            result.add(new Field(leaf.id(),leaf.key(),type,node.getValue(),leaf.key().toLowerCase(Locale.ROOT).matches(".*(password|passwd|secret|token|private[-_.]?key|credential).*")));}
        return result;
    }
    /** Changes only edited scalar spans, preserving comments, ordering and other YAML byte-for-byte. */
    public static String update(String yaml,Map<String,Field> changes){
        record Edit(int start,int end,String value){}List<Edit> edits=new ArrayList<>();Set<String> found=new HashSet<>();
        for(var leaf:leaves(yaml)){Field value=changes.get(leaf.id());if(value==null)continue;found.add(leaf.id());
            ScalarNode node=leaf.node();String scalar=encode(value);
            int start=yaml.offsetByCodePoints(0,node.getStartMark().getIndex()),end=yaml.offsetByCodePoints(0,node.getEndMark().getIndex());
            // Block scalars include the following newline in their span; retain it before the next key.
            String replaced=yaml.substring(start,end);if(replaced.endsWith("\n"))scalar+=replaced.endsWith("\r\n")?"\r\n":"\n";
            edits.add(new Edit(start,end,scalar));
        }
        if(!found.equals(changes.keySet()))throw new IllegalArgumentException("Unknown field");
        edits.sort(Comparator.comparingInt(Edit::start).reversed());StringBuilder result=new StringBuilder(yaml);
        edits.forEach(edit->result.replace(edit.start(),edit.end(),edit.value()));validate(result.toString());return result.toString();
    }
    private static String encode(Field field){
        String value=field.value();if(value==null||value.length()>MAX_BYTES)throw new IllegalArgumentException("Invalid value");
        switch(field.type()){
            case "string": {DumperOptions options=new DumperOptions();options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);return new Yaml(options).dump(value).strip();}
            case "boolean": if(value.equals("true")||value.equals("false"))return value;break;
            case "null": if(value.isBlank()||value.equals("null")||value.equals("~"))return "null";break;
            case "number": {
                var documents=new Yaml(options()).composeAll(new StringReader(value)).iterator();if(!documents.hasNext())break;
                Node node=documents.next();if(!documents.hasNext()&&node instanceof ScalarNode scalar&&(Tag.INT.equals(scalar.getTag())||Tag.FLOAT.equals(scalar.getTag())))return value;break;
            }
            default: break;
        }
        throw new IllegalArgumentException("Invalid value/type");
    }
    public static void validate(String yaml){leaves(yaml);}
    private static LoaderOptions options(){LoaderOptions o=new LoaderOptions();o.setAllowDuplicateKeys(false);o.setMaxAliasesForCollections(0);o.setCodePointLimit(MAX_BYTES);return o;}
    private static List<Leaf> leaves(String yaml){
        if(yaml.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("YAML exceeds 1 MB");
        List<Leaf> leaves=new ArrayList<>();int index=0;Set<Node> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Node document:new Yaml(options()).composeAll(new StringReader(yaml))){
            if(!(document instanceof MappingNode))throw new IllegalArgumentException("Each YAML document must contain a mapping");
            walk(document,"",index++,leaves,seen,new HashSet<>());
        }
        return leaves;
    }
    private static void walk(Node node,String path,int doc,List<Leaf> leaves,Set<Node> seen,Set<String> paths){
        if(!seen.add(node))throw new IllegalArgumentException("YAML aliases/merges require manual editing outside this editor");
        if(node instanceof MappingNode map){if(!Tag.MAP.equals(node.getTag()))throw new IllegalArgumentException("Unsupported YAML tag");Set<String> keys=new HashSet<>();
            for(var tuple:map.getValue()){if(!(tuple.getKeyNode() instanceof ScalarNode key)||!Tag.STR.equals(key.getTag())||!keys.add(key.getValue())||key.getValue().equals("<<"))throw new IllegalArgumentException("Duplicate/unsupported YAML key");
                walk(tuple.getValueNode(),path.isEmpty()?key.getValue():path+"."+key.getValue(),doc,leaves,seen,paths);}
        }else if(node instanceof SequenceNode sequence){if(!Tag.SEQ.equals(node.getTag()))throw new IllegalArgumentException("Unsupported YAML tag");int i=0;for(Node item:sequence.getValue())walk(item,path+"["+i+++"]",doc,leaves,seen,paths);
        }else if(node instanceof ScalarNode scalar){if(!List.of(Tag.STR,Tag.INT,Tag.FLOAT,Tag.BOOL,Tag.NULL,Tag.TIMESTAMP).contains(scalar.getTag()))throw new IllegalArgumentException("Unsupported YAML tag");
            if(!paths.add(path))throw new IllegalArgumentException("Conflicting flattened keys");leaves.add(new Leaf(doc+":"+path,(doc==0?"":"["+(doc+1)+"] ")+path,scalar));
        }else throw new IllegalArgumentException("Unsupported YAML node");
    }
    private static byte[] readBytes(Path file)throws IOException {
        try(var input=Files.newInputStream(file)){byte[] bytes=input.readNBytes(MAX_BYTES+1);if(bytes.length>MAX_BYTES)throw new IOException("YAML file exceeds 1 MB");return bytes;}
    }
    private static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception error){throw new IllegalStateException(error);}}
}
