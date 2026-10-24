package com.turkerozturk.multipledatabases;

import java.nio.file.*;
import java.util.*;

/** Filesystem metadata only: never opens a pool or creates a missing SQLite file. */
final class TenantFileMetadata {
    private TenantFileMetadata() { }
    static Path ctbPath(String url) {
        if(url==null || !url.startsWith("jdbc:sqlite:"))return null;
        String path=url.substring("jdbc:sqlite:".length());
        if(path.contains("mode=memory") || path.equals(":memory:"))return null;
        try {
            if(path.startsWith("file:")) {
                var uri=java.net.URI.create(path);
                path=uri.getPath();
                if(path==null)path=uri.getSchemeSpecificPart().split("\\?",2)[0];
            }
            path=path.split("\\?",2)[0];
            if(!path.toLowerCase(Locale.ROOT).endsWith(".ctb"))return null;
            return Path.of(path).toAbsolutePath().normalize();
        }catch(RuntimeException error){return null;}
    }
    static Long modified(String url) {
        Path path=ctbPath(url);if(path==null)return null;
        try{return Files.isRegularFile(path)?Files.getLastModifiedTime(path).toMillis():null;}
        catch(java.io.IOException|SecurityException error){return null;}
    }
    static final Comparator<String> NAMES=String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());
}
