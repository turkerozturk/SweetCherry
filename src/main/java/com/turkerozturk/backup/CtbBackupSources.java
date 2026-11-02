package com.turkerozturk.backup;

import com.turkerozturk.multipledatabases.*;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/** Resolves registered configuration only. Never activates a tenant, creates a CTB or performs schema upgrades. */
@Component
public class CtbBackupSources {
    public record Source(String key,Path path,List<String> names,String error) {}
    private final TenantService tenants;
    public CtbBackupSources(TenantService tenants){this.tenants=tenants;}
    static Path configured(DataSource source) {
        if(source instanceof ManagedTenantDataSource managed)source=managed.configuration();
        if(source instanceof HikariDataSource pool)return TenantFileMetadata.ctbPath(pool.getJdbcUrl());
        if(source instanceof org.sqlite.SQLiteDataSource sqlite)return TenantFileMetadata.ctbPath(sqlite.getUrl());
        return null;
    }
    static String key(Path path) throws IOException {
        String canonical=path.toRealPath().toString();
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    List<Path> protectedFiles() throws IOException {
        List<Path> result=new ArrayList<>();
        for(DataSource source:tenants.getAllTenants().values()) {
            Path path=configured(source);if(path!=null && Files.exists(path))result.add(path.toRealPath());
        }
        return List.copyOf(result);
    }
    List<Source> resolve(List<String> names) {
        var registered=tenants.getAllTenants();List<Source> result=new ArrayList<>();
        for(String name:new TreeSet<>(names)) {
            try {
                DataSource source=registered.get(name);
                if(source==null){result.add(new Source(null,null,List.of(name),"ctbBackup.auto.missing"));continue;}
                Path path=configured(source);
                if(path==null){result.add(new Source(null,null,List.of(name),"ctbBackup.error.unsupported"));continue;}
                path=path.toRealPath();if(!Files.isRegularFile(path))throw new IOException();
                boolean merged=false;
                for(int i=0;i<result.size();i++) {
                    Source existing=result.get(i);
                    if(existing.path()!=null && Files.isSameFile(existing.path(),path)) {
                        var aliases=new ArrayList<>(existing.names());aliases.add(name);
                        Path representative=existing.path().toString().compareTo(path.toString())<=0?existing.path():path;
                        result.set(i,new Source(key(representative),representative,List.copyOf(aliases),null));merged=true;break;
                    }
                }
                if(!merged)result.add(new Source(key(path),path,List.of(name),null));
            }catch(IOException|RuntimeException failed){result.add(new Source(null,null,List.of(Objects.requireNonNullElse(name,"")),"ctbBackup.error.source"));}
        }
        return List.copyOf(result);
    }
}
