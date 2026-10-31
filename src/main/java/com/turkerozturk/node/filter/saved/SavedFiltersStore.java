package com.turkerozturk.node.filter.saved;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkerozturk.helpers.NodeIcon;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;

/** Private per-user/source preferences; never writes to the note database. */
@Service
public class SavedFiltersStore {
    public static final Set<String> FIELDS=Set.of("nameFilter","nameStartsWith","textFilter","tagsFilter","syntaxFilter",
            "nodeIdStartFilter","nodeIdEndFilter","tsCreationStartFilter","tsCreationEndFilter",
            "tsModificationStartFilter","tsModificationEndFilter","nodeIcons","pageSize");
    private static final int MAX_BYTES=1_048_576;
    public record Preset(String id,String name,Map<String,List<String>> fields) { }
    private record Document(int version,List<Preset> presets) { }
    public record Snapshot(String scope,String revision,List<Preset> presets) { }
    public static final class Conflict extends RuntimeException { }
    private final ObjectMapper mapper;
    private final Path directory;
    @org.springframework.beans.factory.annotation.Autowired
    public SavedFiltersStore(ObjectMapper mapper){this(mapper,Path.of("saved-filters"));}
    SavedFiltersStore(ObjectMapper mapper,Path directory){this.mapper=mapper;this.directory=directory.toAbsolutePath().normalize();}
    private Path file(String scope) throws IOException {
        if(scope==null||!scope.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid scope");
        Path file=directory.resolve(scope+".json");
        if(Files.isSymbolicLink(directory)||Files.isSymbolicLink(file))throw new IOException("Symbolic preference paths are not supported");
        return file;
    }
    public synchronized Snapshot read(String scope) throws IOException {
        Path file=file(scope);
        if(!Files.exists(file))return new Snapshot(scope,hash(new byte[0]),List.of());
        if(!Files.isRegularFile(file)||Files.size(file)>MAX_BYTES)throw new IOException("Invalid preference file");
        byte[] bytes;try(var in=Files.newInputStream(file)){bytes=in.readNBytes(MAX_BYTES+1);}
        if(bytes.length>MAX_BYTES)throw new IOException("Preference file too large");
        Document doc=mapper.readValue(bytes,Document.class);
        try {
            if(doc.version()!=1||doc.presets()==null||doc.presets().size()>50)throw new IllegalArgumentException();
            Set<String> ids=new HashSet<>(),names=new HashSet<>();
            for(Preset p:doc.presets()) {
                if(p==null||!uuid(p.id())||!ids.add(p.id())||!names.add(name(p.name()).toLowerCase(Locale.ROOT)))throw new IllegalArgumentException();
                validateFields(p.fields());
            }
        }catch(RuntimeException invalid){throw new IOException("Invalid saved filters",invalid);}
        return new Snapshot(scope,hash(bytes),List.copyOf(doc.presets()));
    }
    public synchronized Snapshot save(String scope,String revision,String id,String name,Map<String,List<String>> fields) throws IOException {
        String label=name(name);Map<String,List<String>> values=validateFields(fields);final String supplied=id;
        Snapshot current=read(scope);checkRevision(current,revision);
        var presets=new ArrayList<>(current.presets());
        if(id==null||id.isBlank()){if(presets.size()>=50)throw new IllegalArgumentException("At most 50 filters");id=UUID.randomUUID().toString();}
        else if(!uuid(id)||presets.stream().noneMatch(p->p.id().equals(supplied)))throw new IllegalArgumentException("Unknown filter");
        final String key=id;
        if(presets.stream().anyMatch(p->!p.id().equals(key)&&p.name().equalsIgnoreCase(label)))throw new IllegalArgumentException("Duplicate name");
        presets.removeIf(p->p.id().equals(key));presets.add(new Preset(key,label,values));
        presets.sort(Comparator.comparing(Preset::name,String.CASE_INSENSITIVE_ORDER));
        return write(scope,current,new Document(1,List.copyOf(presets)));
    }
    public synchronized Snapshot delete(String scope,String revision,String id) throws IOException {
        Snapshot current=read(scope);checkRevision(current,revision);
        var presets=new ArrayList<>(current.presets());
        if(!uuid(id)||!presets.removeIf(p->p.id().equals(id)))throw new IllegalArgumentException("Unknown filter");
        return write(scope,current,new Document(1,List.copyOf(presets)));
    }
    private Snapshot write(String scope,Snapshot current,Document document) throws IOException {
        byte[] bytes=mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(document);
        if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Saved filters exceed 1 MiB");
        if(!Files.exists(directory)) {
            try{Files.createDirectory(directory,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));}
            catch(UnsupportedOperationException windows){Files.createDirectory(directory);}
            catch(FileAlreadyExistsException concurrent){/* Checked by file() below. */}
        }
        Path target=file(scope),temporary=Files.createTempFile(directory,".saved-",".tmp");
        try {
            if(Files.getFileStore(directory).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(temporary,PosixFilePermissions.fromString("rw-------"));
            Files.write(temporary,bytes);
            checkRevision(read(scope),current.revision());
            try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException fallback){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
        return new Snapshot(scope,hash(bytes),document.presets());
    }
    private static void checkRevision(Snapshot value,String revision){if(!value.revision().equals(revision))throw new Conflict();}
    private static boolean uuid(String value){return value!=null&&value.matches("[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}");}
    private static String name(String value){
        if(value==null||value.isBlank()||value.length()>80||value.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid name");
        return value.strip();
    }
    static Map<String,List<String>> validateFields(Map<String,List<String>> fields){
        if(fields==null||!FIELDS.equals(fields.keySet()))throw new IllegalArgumentException("Invalid fields");
        Map<String,List<String>> clean=new TreeMap<>();
        for(String key:FIELDS){
            List<String> values=fields.get(key);if(values==null||values.size()>(key.equals("nodeIcons")?NodeIcon.values().length:1))throw new IllegalArgumentException("Invalid field values");
            for(String value:values){
                if(value==null||value.length()>8192||value.indexOf('\0')>=0)throw new IllegalArgumentException("Invalid value");
                if(key.equals("nodeIcons"))NodeIcon.valueOf(value);
                if(key.equals("nameStartsWith")&&!Set.of("true","false").contains(value))throw new IllegalArgumentException("Invalid checkbox");
            }
            clean.put(key,List.copyOf(values));
        }
        return Collections.unmodifiableMap(clean);
    }
    private static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception impossible){throw new IllegalStateException(impossible);}}
}
