package com.turkerozturk.settings.editing;

import org.springframework.stereotype.Service;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.TextResourceOrigin;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.nodes.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.io.*;
import com.turkerozturk.sunandmoon.AstronomyCoordinates;

@Service
public class ApplicationSettingsService {
    public record Setting(String key,String type,String fallback,long maximum) {}
    public static final List<Setting> SETTINGS=List.of(
        new Setting("astronomy.enabled","boolean","true",0),
        new Setting("astronomy.latitude","latitude","40.9893",0),
        new Setting("astronomy.longitude","longitude","29.0373",0),
        new Setting("astronomy.timezone","timezone","Europe/Istanbul",0),
        new Setting("myapp.openWebBrowserOnStartup","boolean","false",0),
        new Setting("myapp.syntax-highlighting.enabled","boolean","true",0),
        new Setting("myapp.debug","boolean","false",0),
        new Setting("myapp.pdf.max-nodes","number","512",100000),
        new Setting("myapp.pdf.max-depth","number","64",256),
        new Setting("myapp.pdf.max-node-text-characters","number","8388608",2147483647),
        new Setting("myapp.pdf.max-html-characters","number","67108864",2147483647),
        new Setting("myapp.pdf.max-image-bytes","number","50331648",2147483647),
        new Setting("myapp.pdf.max-image-pixels","number","40000000",2147483647));
    private final ConfigurableEnvironment environment;
    public ApplicationSettingsService(ConfigurableEnvironment environment){this.environment=environment;}
    public Path file()throws IOException {
        // Only edit a YAML file that Spring actually loaded, never a caller-supplied path or a classpath resource.
        Set<Path> candidates=new LinkedHashSet<>();
        for(var source:environment.getPropertySources()) {
            if(source.getName().contains("classpath:"))continue;
            if(source instanceof org.springframework.core.env.EnumerablePropertySource<?> enumerable)
                for(String key:enumerable.getPropertyNames()) {
                    var origin=OriginLookup.getOrigin(source,key);
                    if(origin instanceof TextResourceOrigin text && text.getResource().isFile() && !(text.getResource() instanceof org.springframework.core.io.ClassPathResource)) {
                        Path path=text.getResource().getFile().toPath().toRealPath();
                        String name=path.getFileName().toString();
                        if(name.equals("application.yml")||name.equals("application.yaml"))candidates.add(path);
                    }
                }
        }
        if(candidates.size()!=1)throw new IOException("Exactly one active external application.yml/application.yaml is required");
        Path path=candidates.iterator().next();
        if(!Files.isWritable(path)||Files.size(path)>1048576)throw new IOException("Configuration is not writable or too large");
        return path;
    }
    public record Snapshot(String path,String revision,Map<String,String> values,Map<String,String> effective){}
    public Snapshot read()throws IOException {
        Path file=file();String yaml=Files.readString(file,StandardCharsets.UTF_8);Map<String,ScalarNode> nodes=nodes(yaml);
        Map<String,String> values=new LinkedHashMap<>(),effective=new LinkedHashMap<>();
        for(var setting:SETTINGS){values.put(setting.key(),nodes.containsKey(setting.key())?nodes.get(setting.key()).getValue():environment.getProperty(setting.key(),setting.fallback()));effective.put(setting.key(),environment.getProperty(setting.key(),setting.fallback()));}
        return new Snapshot(file.toString(),hash(yaml),values,effective);
    }
    public synchronized void save(String revision,Map<String,String> values)throws IOException {
        Path file=file();String before=Files.readString(file,StandardCharsets.UTF_8);
        if(!hash(before).equals(revision))throw new IllegalArgumentException("stale");
        String after=update(before,values);
        Path temporary=Files.createTempFile(file.getParent(),".sweetcherry-settings-",".tmp");
        try {
            if(Files.getFileStore(file).supportsFileAttributeView("posix"))Files.setPosixFilePermissions(temporary,Files.getPosixFilePermissions(file));
            var originalAcl=Files.getFileAttributeView(file,java.nio.file.attribute.AclFileAttributeView.class);
            var temporaryAcl=Files.getFileAttributeView(temporary,java.nio.file.attribute.AclFileAttributeView.class);
            if(originalAcl!=null && temporaryAcl!=null)temporaryAcl.setAcl(originalAcl.getAcl());
            Files.writeString(temporary,after,StandardCharsets.UTF_8);
            if(!hash(Files.readString(file,StandardCharsets.UTF_8)).equals(revision))throw new IllegalArgumentException("stale");
            Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(temporary);}
    }
    public static String update(String yaml,Map<String,String> values) {
        Set<String> allowed=new HashSet<>();SETTINGS.forEach(s->allowed.add(s.key()));
        if(!allowed.equals(values.keySet()))throw new IllegalArgumentException("invalid");
        var nodes=nodes(yaml);record Edit(int start,int end,String value){}List<Edit> edits=new ArrayList<>();StringBuilder extra=new StringBuilder();
        for(var setting:SETTINGS){String value=values.get(setting.key());validate(setting,value);
            String scalar=setting.type().equals("boolean")||setting.type().equals("number")?value:"'"+value.replace("'","''")+"'";
            ScalarNode node=nodes.get(setting.key());
            if(node==null)extra.append(setting.key()).append(": ").append(scalar).append("\n");
            else edits.add(new Edit(yaml.offsetByCodePoints(0,node.getStartMark().getIndex()),yaml.offsetByCodePoints(0,node.getEndMark().getIndex()),scalar));
        }
        edits.sort(Comparator.comparingInt(Edit::start).reversed());StringBuilder result=new StringBuilder(yaml);
        for(var edit:edits)result.replace(edit.start(),edit.end(),edit.value());
        if(!extra.isEmpty())result.append("\n# Settings added by SweetCherry\n").append(extra);
        nodes(result.toString());return result.toString();
    }
    private static void validate(Setting setting,String value){
        if(value==null||value.length()>160||value.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("invalid");
        switch(setting.type()) {
            case "boolean" -> {if(!value.equals("true")&&!value.equals("false"))throw new IllegalArgumentException("invalid");}
            case "number" -> {if(!value.matches("[1-9][0-9]*")||Long.parseLong(value)>setting.maximum())throw new IllegalArgumentException("invalid");}
            case "latitude" -> AstronomyCoordinates.parse(value,true);
            case "longitude" -> AstronomyCoordinates.parse(value,false);
            case "timezone" -> java.time.ZoneId.of(value);
            default -> throw new IllegalArgumentException("invalid");
        }
    }
    private static Map<String,ScalarNode> nodes(String yaml){
        LoaderOptions options=new LoaderOptions();options.setAllowDuplicateKeys(false);options.setMaxAliasesForCollections(0);options.setCodePointLimit(1048576);
        var parser=new Yaml(options);var documents=parser.composeAll(new StringReader(yaml)).iterator();
        if(!documents.hasNext())throw new IllegalArgumentException("invalid");Node root=documents.next();
        if(documents.hasNext()||!(root instanceof MappingNode))throw new IllegalArgumentException("invalid");
        Map<String,ScalarNode> result=new HashMap<>();walk(root,"",result,new HashSet<>());return result;
    }
    private static void walk(Node node,String prefix,Map<String,ScalarNode> result,Set<Node> seen){
        if(!seen.add(node))throw new IllegalArgumentException("invalid");
        if(node instanceof MappingNode mapping){Set<String> keys=new HashSet<>();
            for(var entry:mapping.getValue()){if(!(entry.getKeyNode() instanceof ScalarNode key)||!keys.add(key.getValue())||key.getValue().equals("<<"))throw new IllegalArgumentException("invalid");
                walk(entry.getValueNode(),prefix.isEmpty()?key.getValue():prefix+"."+key.getValue(),result,seen);}
        }else if(node instanceof ScalarNode scalar){if(result.put(prefix,scalar)!=null)throw new IllegalArgumentException("invalid");}
        else if(node instanceof SequenceNode sequence){for(Node item:sequence.getValue())walk(item,prefix+"[]",new HashMap<>(),seen);}
    }
    private static String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception error){throw new IllegalStateException(error);}}
}
