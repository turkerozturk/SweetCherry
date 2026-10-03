package com.turkerozturk.node.moving;

import com.turkerozturk.multipledatabases.RequiresTenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Lets authenticated readers detect external hierarchy/title changes without loading content payloads. */
@RestController
@RequiresTenant
public class NavigationRevisionController {
    @PersistenceContext private EntityManager entityManager;
    public record Revision(String revision) { }
    @GetMapping("/nodes/navigation/revision")
    @Transactional(readOnly=true)
    public Revision revision() {
        return new Revision(entityManager.unwrap(org.hibernate.Session.class).doReturningWork(NavigationRevisionController::fingerprint));
    }

    /** Hashes only fields that affect tree rows; no image, attachment or text content is read. */
    static String fingerprint(Connection connection) throws SQLException {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            for(String sql:new String[]{"SELECT node_id,father_id,sequence,master_id FROM children ORDER BY node_id",
                    "SELECT node_id,name,syntax,is_ro,is_richtxt FROM node ORDER BY node_id"}) {
                digest.update(sql.getBytes(StandardCharsets.UTF_8));
                try(var statement=connection.prepareStatement(sql);var rows=statement.executeQuery()) {
                    int count=rows.getMetaData().getColumnCount();
                    while(rows.next()) for(int column=1;column<=count;column++) {
                        String value=rows.getString(column);
                        digest.update((byte)(value==null?0:1));
                        byte[] bytes=value==null?new byte[0]:value.getBytes(StandardCharsets.UTF_8);
                        digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException error) {throw new IllegalStateException(error);}
    }
}
