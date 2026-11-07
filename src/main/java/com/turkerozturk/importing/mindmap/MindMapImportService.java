package com.turkerozturk.importing.mindmap;

import com.turkerozturk.node.properties.NodePropertiesService;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;

@Service
public class MindMapImportService {
    @PersistenceContext private EntityManager entityManager;
    private final NodePropertiesService properties;
    public MindMapImportService(NodePropertiesService properties){this.properties=properties;}
    private void writable(){if(!properties.writable())throw new AccessDeniedException("Selected database is read-only");}
    public MindMapImportData preview(InputStream input)throws java.io.IOException {
        writable();return MindMapImportData.read(input.readNBytes(MindMapImportData.MAX_BYTES+1));
    }
    @Transactional(rollbackFor=Exception.class)
    public synchronized int confirm(MindMapImportData data) {
        writable();
        int count=entityManager.unwrap(Session.class).doReturningWork(data::insert);
        entityManager.clear();return count;
    }
}
