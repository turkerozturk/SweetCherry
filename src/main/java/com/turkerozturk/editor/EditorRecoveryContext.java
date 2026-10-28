package com.turkerozturk.editor;

import com.turkerozturk.multipledatabases.*;
import com.turkerozturk.node.contentediting.NodeContentEditingController;
import com.turkerozturk.richtext.editing.RichTextEditingController;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.web.bind.annotation.*;

/** Stable, opaque recovery namespace; never emits JDBC URLs, credentials or session tokens. */
@ControllerAdvice(assignableTypes={NodeContentEditingController.class,RichTextEditingController.class})
public class EditorRecoveryContext {
    private final TenantService tenants;
    public EditorRecoveryContext(TenantService tenants){this.tenants=tenants;}
    @ModelAttribute("editorRecoveryScope")
    public String scope(HttpServletRequest request) {
        String tenant=TenantContext.getCurrentTenant();
        if(tenant==null || request.getUserPrincipal()==null)return "";
        var source=tenants.getAllTenants().get(tenant);
        if(source instanceof ManagedTenantDataSource managed)source=managed.configuration();
        // Do not enable durable recovery if the database identity is unknown.
        if(!(source instanceof HikariDataSource hikari) || hikari.getJdbcUrl()==null)return "";
        String installation=java.nio.file.Path.of("").toAbsolutePath().normalize().toString();
        return fingerprint(request.getUserPrincipal().getName(),tenant,installation+"\0"+hikari.getJdbcUrl());
    }
    static String fingerprint(String user,String tenant,String url) {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            for(String value:new String[]{user,tenant,url}) {
                byte[] bytes=value.getBytes(StandardCharsets.UTF_8);
                digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
