package com.turkerozturk.multipledatabases;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

@Service
public class TenantWizardService {
    private final TenantService tenants;
    private final CustomPropertiesHolder custom;
    private final MultitenantConfiguration configuration;
    private final Path directory;
    private static final Map<String,String> DRIVERS = Map.of("sqlite","org.sqlite.JDBC","mysql","com.mysql.cj.jdbc.Driver","mariadb","org.mariadb.jdbc.Driver","postgresql","org.postgresql.Driver");
    @org.springframework.beans.factory.annotation.Autowired
    public TenantWizardService(TenantService tenants, CustomPropertiesHolder custom, MultitenantConfiguration configuration) {
        this(tenants,custom,configuration,Path.of(MultitenantConfiguration.PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES));
    }
    TenantWizardService(TenantService tenants, CustomPropertiesHolder custom, MultitenantConfiguration configuration, Path directory) {
        this.tenants=tenants; this.custom=custom; this.configuration=configuration; this.directory=directory;
    }
    private Path resolve(String tenant) throws IOException {
        Path file=TenantConfigDownloadController.resolve(tenants,custom,directory,tenant);
        Path entry=directory.resolve(custom.getCustomProperties(tenant).get("propertyFileName"));
        if(Files.isSymbolicLink(entry)) throw new IllegalArgumentException("wizard.invalidConfig");
        return file;
    }
    private static Properties read(Path file) throws IOException {
        if(Files.size(file)>65536) throw new IllegalArgumentException("wizard.largeConfig");
        Properties p=new Properties(); try(var in=Files.newBufferedReader(file,StandardCharsets.UTF_8)){p.load(in);} return p;
    }
    static String revision(Path file) throws IOException {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public TenantWizardForm edit(String tenant) throws IOException {
        Path file=resolve(tenant); var p=read(file); var f=new TenantWizardForm();
        f.tenant=tenant; f.revision=revision(file); f.name=p.getProperty("name",tenant); f.url=p.getProperty("datasource.url","");
        f.type=DRIVERS.entrySet().stream().filter(e->e.getValue().equals(p.getProperty("datasource.driver-class-name"))).map(Map.Entry::getKey).findFirst().orElseThrow(()->new IllegalArgumentException("wizard.unsupportedDriver"));
        if(f.type.equals("sqlite")) f.path=f.url.substring("jdbc:sqlite:".length());
        f.username=p.getProperty("datasource.username",""); f.role=p.getProperty("security-role","USER");
        f.writable=Boolean.parseBoolean(p.getProperty("custom.isWritable","false"));
        f.allowLegacySchemaUpgrade=permission(p.getProperty(CtbSchemaCompatibility.SETTING));
        f.newNodeName=p.getProperty("custom.newNodeName",""); f.newNodeTags=p.getProperty("custom.newNodeTags","");
        f.maxEmbeddedFileSizeMB=p.getProperty("custom.maxEmbeddedFileSizeMB","9"); return f;
    }
    private static boolean permission(String s){return s!=null&&(s.trim().equals("1")||s.trim().equalsIgnoreCase("true"));}
    static Path ctb(String input) throws IOException {
        Path path=Path.of(input).toAbsolutePath().normalize().toRealPath();
        if(!Files.isRegularFile(path)||!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ctb")) throw new IllegalArgumentException("wizard.invalidCtb");
        // JDBC SQLite URL metacharacters must never become URI/query/SQL options.
        if(path.toString().contains("?")||path.toString().contains("#")||path.toString().contains("\n")||path.toString().contains("\r")) throw new IllegalArgumentException("wizard.invalidPath");
        try(var in=Files.newInputStream(path)) {
            if(!Arrays.equals(in.readNBytes(16),"SQLite format 3\0".getBytes(StandardCharsets.US_ASCII))) throw new IllegalArgumentException("wizard.invalidCtb");
        }
        return path;
    }
    public synchronized String save(TenantWizardForm f) throws IOException {
        for(String value:new String[]{f.username,f.password,f.newNodeName,f.newNodeTags})
            if(value==null||value.length()>1024)throw new IllegalArgumentException("wizard.invalidConfig");
        if(!DRIVERS.containsKey(f.type)||!Set.of("ADMIN","USER").contains(f.role)) throw new IllegalArgumentException("wizard.invalidConfig");
        boolean editing=f.tenant!=null&&!f.tenant.isBlank(); Path file=editing?resolve(f.tenant):null;
        Properties p=editing?read(file):new Properties();
        if(editing&&!revision(file).equals(f.revision)) throw new IllegalArgumentException("wizard.stale");
        String url;
        if(f.type.equals("sqlite")) {
            Path database=ctb(f.path); url="jdbc:sqlite:"+database;
            if(f.name==null||f.name.isBlank())f.name=database.getFileName().toString();
        } else {
            url=f.url==null?"":f.url.trim();
            if(!url.startsWith("jdbc:"+f.type+"://")||url.contains("\n")||url.contains("\r")||url.length()>4000)throw new IllegalArgumentException("wizard.invalidUrl");
        }
        f.name=f.name==null?"":f.name.trim();
        if(f.name.isBlank()||f.name.length()>160||f.name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("wizard.invalidName");
        if(tenants.getAllTenants().containsKey(f.name)&&!f.name.equals(f.tenant))throw new IllegalArgumentException("wizard.duplicateName");
        try { int limit=Integer.parseInt(f.maxEmbeddedFileSizeMB); if(limit<1||limit>20)throw new NumberFormatException(); }
        catch(RuntimeException e){throw new IllegalArgumentException("wizard.invalidLimit");}
        p.setProperty("name",f.name);p.setProperty("datasource.driver-class-name",DRIVERS.get(f.type));p.setProperty("datasource.url",url);
        p.setProperty("security-role",f.role);p.setProperty("custom.isWritable",String.valueOf(f.writable));
        p.setProperty(CtbSchemaCompatibility.SETTING,String.valueOf(f.allowLegacySchemaUpgrade));
        p.setProperty("custom.newNodeName",f.newNodeName);p.setProperty("custom.newNodeTags",f.newNodeTags);p.setProperty("custom.maxEmbeddedFileSizeMB",f.maxEmbeddedFileSizeMB);
        p.setProperty("datasource.username",f.username);
        if(f.clearPassword)p.remove("datasource.password");else if(f.password!=null&&!f.password.isEmpty())p.setProperty("datasource.password",f.password);
        Files.createDirectories(directory);
        if(!editing)file=directory.resolve("tenant-"+UUID.randomUUID()+".txt");
        var writer=new StringWriter();p.store(writer,"SweetCherry tenant configuration");
        if(writer.toString().getBytes(StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("wizard.largeConfig");
        configuration.installTenantConfig(editing?f.tenant:null,file,writer.toString().getBytes(StandardCharsets.UTF_8),p,editing?f.revision:null);
        return f.name;
    }
}
