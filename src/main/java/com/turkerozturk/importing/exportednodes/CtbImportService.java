package com.turkerozturk.importing.exportednodes;

import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.util.*;

@Service
public class CtbImportService {
    @PersistenceContext private EntityManager entityManager;
    private final NodePropertiesService properties;
    @Value("${user.dir}") private String applicationPath;
    @Value("${myapp.exportingFolderName}") private String folder;
    public CtbImportService(NodePropertiesService properties) { this.properties=properties; }
    public record Preview(String filename,String digest,Map<String,Integer> counts) { }
    void writable() { if(!properties.writable()) throw new AccessDeniedException("Selected database is read-only"); }
    private CtbImportData read(String filename) throws Exception {
        writable();
        if(filename==null || filename.startsWith(".") || !filename.matches("[^\\\\/:*?\"<>|\\p{Cntrl}]+")
                || !filename.toLowerCase(Locale.ROOT).endsWith(".ctb")) throw new IllegalArgumentException("file");
        Path directory=Path.of(applicationPath,folder).toAbsolutePath().normalize();
        Path file=directory.resolve(filename);
        if(Files.isSymbolicLink(directory) || !Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
                || !file.toRealPath().getParent().equals(directory.toRealPath())) throw new IllegalArgumentException("file");
        if(Files.size(file)>64L*1024*1024) throw new IllegalArgumentException("limit");
        return CtbImportData.read(file);
    }
    public synchronized Preview preview(String filename) throws Exception {
        var data=read(filename); return new Preview(filename,data.digest,data.counts());
    }
    @Transactional(rollbackFor=Exception.class)
    public synchronized int confirm(Preview preview) throws Exception {
        var data=read(preview.filename());
        if(!data.digest.equals(preview.digest())) throw new IllegalArgumentException("changed");
        int count=entityManager.unwrap(Session.class).doReturningWork(connection->{
            if(connection.isReadOnly()) throw new AccessDeniedException("Selected database is read-only");
            return data.insert(connection);
        });
        entityManager.clear(); return count;
    }
}
