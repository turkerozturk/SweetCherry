package com.turkerozturk.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Component;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.*;

/** Only generated identifiers are accepted; incomplete snapshots have no published manifest. */
@Component
public class CtbBackupStore {
    public record Backup(String id,String fileName,String sourceName,String created,long bytes,String sha256,Map<String,Long> counts) {
        public String date() { return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.parse(created)); }
    }
    public record Catalog(List<Backup> backups,int invalid) {}
    private final Path configured;
    private final ObjectMapper json=new ObjectMapper();
    public CtbBackupStore(CtbBackupSettings settings) { configured=Path.of(settings.getDirectory()).toAbsolutePath().normalize(); }
    public String directory() { return configured.toString(); }
    Path root() throws IOException {
        if (!Files.exists(configured)) {
            try { Files.createDirectories(configured,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))); }
            catch(UnsupportedOperationException unsupported) { Files.createDirectories(configured); }
        }
        return configured.toRealPath();
    }
    static void id(String id) {
        if(id==null || !id.matches("[0-9]{8}-[0-9]{6}-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) throw new CtbBackupProblem("missing");
    }
    Path temp(String suffix) throws IOException {
        try { return Files.createTempFile(root(),"pending-",suffix,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))); }
        catch(UnsupportedOperationException unsupported) { return Files.createTempFile(root(),"pending-",suffix); }
    }
    Path file(String id) throws IOException { id(id);return root().resolve("backup-"+id+".ctb"); }
    private Path metadata(String id) throws IOException { id(id);return root().resolve("backup-"+id+".json"); }
    static void move(Path from,Path to) throws IOException {
        try { Files.move(from,to,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException unsupported) { Files.move(from,to); }
    }
    void publish(Backup backup) throws IOException {
        Path pending=temp(".json.partial");
        try { json.writeValue(pending.toFile(),backup);force(pending);move(pending,metadata(backup.id())); }
        finally { Files.deleteIfExists(pending); }
    }
    Backup get(String id) throws IOException {
        Path meta=metadata(id);Path file=file(id);
        if (!Files.isRegularFile(meta,NOFOLLOW_LINKS) || Files.size(meta)>65536 || !Files.isRegularFile(file,NOFOLLOW_LINKS)) throw new CtbBackupProblem("missing");
        Backup backup;
        try(var in=Files.newInputStream(meta,READ,NOFOLLOW_LINKS)) { backup=json.readValue(in,Backup.class); }
        if (!id.equals(backup.id()) || !file.getFileName().toString().equals(backup.fileName()) || backup.bytes()!=Files.size(file)
                || backup.sha256()==null || !backup.sha256().matches("[a-f0-9]{64}") || backup.sourceName()==null || backup.created()==null || backup.counts()==null)
            throw new CtbBackupProblem("integrity");
        try { java.time.Instant.parse(backup.created()); } catch(java.time.DateTimeException invalid) { throw new CtbBackupProblem("integrity"); }
        return backup;
    }
    public Catalog catalog() throws IOException {
        List<Backup> backups=new ArrayList<>();int invalid=0;
        if (!Files.exists(configured)) return new Catalog(List.of(),0);
        try(var paths=Files.newDirectoryStream(root(),"backup-*.json")) {
            for(Path path:paths) try {
                String name=path.getFileName().toString();backups.add(get(name.substring(7,name.length()-5)));
            } catch(IOException|RuntimeException rejected) { invalid++; }
        }
        backups.sort(Comparator.comparing(Backup::created).reversed().thenComparing(Backup::id));
        return new Catalog(List.copyOf(backups),invalid);
    }
    static String sha256(Path file) throws IOException {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            try(var in=Files.newInputStream(file,READ,NOFOLLOW_LINKS)) {
                byte[] buffer=new byte[65536];int size;while((size=in.read(buffer))!=-1)digest.update(buffer,0,size);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    Backup checked(String id) throws IOException {
        Backup backup=get(id);
        if(!backup.sha256().equals(sha256(file(id))))throw new CtbBackupProblem("integrity");
        return backup;
    }
    Path workingCopy(String id,CtbBackupEngine engine) throws Exception {
        Backup backup=checked(id);Path copy=temp(".ctb");
        try {
            try(var in=Files.newInputStream(file(id),READ,NOFOLLOW_LINKS);var out=Files.newOutputStream(copy,WRITE,TRUNCATE_EXISTING,NOFOLLOW_LINKS)) { in.transferTo(out); }
            if(!backup.sha256().equals(sha256(copy)))throw new CtbBackupProblem("integrity");
            engine.verify(copy);force(copy);
            Path folder=root().resolve("working-copies");
            if (!Files.exists(folder)) {
                try { Files.createDirectory(folder,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))); }
                catch(UnsupportedOperationException unsupported) { Files.createDirectory(folder); }
            }
            if(Files.isSymbolicLink(folder) || !Files.isDirectory(folder,NOFOLLOW_LINKS))throw new CtbBackupProblem("storage");
            Path target=folder.resolve("restored-"+UUID.randomUUID()+".ctb");move(copy,target);return target;
        } finally { Files.deleteIfExists(copy); }
    }
    static void force(Path file) throws IOException { try(var channel=FileChannel.open(file,WRITE,NOFOLLOW_LINKS)){channel.force(true);} }
}
