package com.turkerozturk.multipledatabases;

import com.turkerozturk.export.texttree.TextTreePaths;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.text.Normalizer;
import java.util.*;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

/** An exclusively created CTB owned by one wizard save. Failure removes only this new database. */
final class EmptyCtbCreation implements AutoCloseable {
    private final Path file;private boolean kept;
    private EmptyCtbCreation(Path file){this.file=file;}
    static String filename(String input) {
        if(input==null||input.isBlank()||!input.equals(input.strip())||input.endsWith("."))throw problem("filename");
        String name=input.toLowerCase(Locale.ROOT).endsWith(".ctb")?input:input+".ctb";
        if(name.startsWith(".")||name.contains("#")||name.codePoints().anyMatch(Character::isISOControl)||!name.equals(TextTreePaths.safe(name,0)))throw problem("filename");
        String stem=Normalizer.normalize(name.split("\\.",2)[0],Normalizer.Form.NFKC).strip();
        if(stem.matches("(?i)CON|PRN|AUX|NUL|CONIN\\$|CONOUT\\$|CLOCK\\$|COM[1-9]|LPT[1-9]"))throw problem("filename");
        return name;
    }
    static Path target(Path directory,String filename)throws IOException {
        Path folder=directory.toAbsolutePath().normalize();
        if(Files.isSymbolicLink(folder))throw problem("directory");
        if(Files.exists(folder,LinkOption.NOFOLLOW_LINKS)&&!Files.isDirectory(folder))throw problem("directory");
        Path target=folder.resolve(filename).normalize();
        if(target.toString().matches("(?s).*[?#\\r\\n].*"))throw new IllegalArgumentException("wizard.invalidPath");
        return target;
    }
    static String key(Path file) {
        Path path=file.toAbsolutePath().normalize();
        // Canonicalize the nearest existing ancestor even when CTBDATA itself is missing.
        // This also resolves Windows 8.3 aliases and existing directory links.
        try {
            Path ancestor=path;var missing=new ArrayDeque<Path>();
            while(ancestor!=null&&!Files.exists(ancestor)) {
                if(ancestor.getFileName()!=null)missing.addFirst(ancestor.getFileName());
                ancestor=ancestor.getParent();
            }
            if(ancestor!=null) {
                Path resolved=ancestor.toRealPath();
                for(Path part:missing)resolved=resolved.resolve(part);
                path=resolved;
            }
        }catch(IOException ignored){}
        return Normalizer.normalize(path.toString(),Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
    static boolean references(String url,Path target) {
        if(url==null||!url.trim().startsWith("jdbc:sqlite:"))return false;
        String path=url.trim().substring("jdbc:sqlite:".length()).split("[?#]",2)[0];
        if(path.equals(":memory:")||path.isBlank())return false;
        try {
            if(path.startsWith("file:")) {
                var uri=java.net.URI.create(path);
                // Keep a hierarchical file URI intact for the platform provider:
                // getPath() turns file:///C:/... into /C:/..., which is not a Windows path,
                // and also loses UNC authority information.
                if(!uri.isOpaque())return key(Path.of(uri)).equals(key(target));
                // SQLite also accepts relative forms such as file:CTBDATA/notes.ctb.
                path=uri.getSchemeSpecificPart();
            }
            return key(Path.of(path)).equals(key(target));
        }catch(IllegalArgumentException e){return false;}
    }
    static void available(Path target,Path configs,TenantService tenants)throws IOException {
        String targetKey=Normalizer.normalize(target.getFileName().toString(),Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if(Files.isDirectory(target.getParent()))try(var files=Files.newDirectoryStream(target.getParent())) {
            for(Path file:files){String existing=Normalizer.normalize(file.getFileName().toString(),Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);if(existing.equals(targetKey)||existing.equals(targetKey+"-wal")||existing.equals(targetKey+"-shm")||existing.equals(targetKey+"-journal"))throw problem("exists");}
        }
        for(var source:tenants.getAllTenants().values()) {
            String url=source instanceof ManagedTenantDataSource managed?managed.configuration().getJdbcUrl():source instanceof com.zaxxer.hikari.HikariDataSource hikari?hikari.getJdbcUrl():null;
            if(references(url,target))throw problem("referenced");
        }
        // Include configs not currently registered, including references to missing CTBs.
        if(Files.isDirectory(configs))try(var files=Files.newDirectoryStream(configs)) {
            for(Path file:files)if(file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")) {
                if(!Files.isRegularFile(file)||Files.size(file)>65536)throw problem("configCheck");
                var properties=new Properties();try(var input=Files.newBufferedReader(file,StandardCharsets.UTF_8)){properties.load(input);}
                if(references(properties.getProperty("datasource.url"),target))throw problem("referenced");
            }
        }
    }
    static EmptyCtbCreation create(Path target)throws IOException {
        Files.createDirectories(target.getParent());
        if(Files.isSymbolicLink(target.getParent()))throw problem("directory");
        try {Files.createFile(target);}catch(FileAlreadyExistsException e){throw problem("exists");}
        var owned=new EmptyCtbCreation(target);
        try {
            var settings=new SQLiteConfig();settings.resetOpenMode(SQLiteOpenMode.CREATE);
            try(var connection=com.turkerozturk.security.LocalSqliteConnections.open(target,settings);
                var schema=EmptyCtbCreation.class.getResourceAsStream("/schemas/schema-sqlite.sql")) {
                if(schema==null)throw new IOException("Missing CTB schema");
                connection.setAutoCommit(false);
                try(var statement=connection.createStatement()) {
                    for(String sql:new String(schema.readAllBytes(),StandardCharsets.UTF_8).split(";"))if(!sql.isBlank())statement.execute(sql);
                }
                connection.commit();
                CtbSchemaCompatibility.ensure(connection,false,true);
            }
            return owned;
        }catch(SQLException|IOException|RuntimeException e) {
            try{owned.close();}catch(IOException cleanup){e.addSuppressed(cleanup);}
            if(e instanceof IllegalArgumentException known)throw known;
            throw new IOException("Cannot create empty CTB",e);
        }
    }
    void keep(){kept=true;}
    @Override public void close()throws IOException {if(!kept)Files.deleteIfExists(file);}
    private static IllegalArgumentException problem(String reason){return new IllegalArgumentException("wizard.newCtb."+reason);}
}
