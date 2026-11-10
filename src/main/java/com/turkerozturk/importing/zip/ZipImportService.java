package com.turkerozturk.importing.zip;

import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.richtext.editing.EmbeddedUploadPolicy;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.file.*;

@Service
public class ZipImportService {
    @PersistenceContext private EntityManager entityManager;
    private final NodePropertiesService properties;private final EmbeddedUploadPolicy attachments;private final MultipartProperties multipart;
    public ZipImportService(NodePropertiesService properties,EmbeddedUploadPolicy attachments,MultipartProperties multipart){this.properties=properties;this.attachments=attachments;this.multipart=multipart;}
    private void writable(){if(!properties.writable())throw new AccessDeniedException("Selected database is read-only");}
    public long uploadBytes(){long limit=multipart.getMaxFileSize().toBytes();return limit<0?ZipImportData.MAX_ARCHIVE:Math.min(limit,ZipImportData.MAX_ARCHIVE);}
    public int attachmentBytes(){return attachments.fileBytes();}
    public ZipImportData preview(InputStream input,ZipImportData.Options options)throws IOException {
        writable();Path archive=Files.createTempFile("sweetcherry-zip-import-",".zip");
        try {
            try(var out=Files.newOutputStream(archive)) {
                byte[] buffer=new byte[8192];long count=0;int n;
                while((n=input.read(buffer))!=-1){count+=n;if(count>uploadBytes())throw new IllegalArgumentException("upload");out.write(buffer,0,n);}
            }
            return ZipImportData.read(archive,options,attachmentBytes());
        }finally{Files.deleteIfExists(archive);}
    }
    @Transactional(rollbackFor=Exception.class)
    public synchronized int confirm(ZipImportData data) {
        writable();
        // Recheck a tenant's attachment policy, which can change after the preview.
        if(data.entries().stream().anyMatch(e->e.action()==ZipImportData.Action.ATTACHMENT&&e.size()>attachmentBytes()))throw new IllegalArgumentException("changed");
        int count=entityManager.unwrap(Session.class).doReturningWork(data::insert);entityManager.clear();return count;
    }
}
