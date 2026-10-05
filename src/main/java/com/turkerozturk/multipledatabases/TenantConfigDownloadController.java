package com.turkerozturk.multipledatabases;

import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/** Downloads registered tenant configuration files for administrators without accepting a caller-supplied path. */
@Controller
public class TenantConfigDownloadController {
    private final TenantService tenants;
    private final CustomPropertiesHolder properties;
    private final Path directory;
    @org.springframework.beans.factory.annotation.Autowired
    public TenantConfigDownloadController(TenantService tenants, CustomPropertiesHolder properties) {
        this(tenants, properties, Path.of(MultitenantConfiguration.PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES));
    }
    TenantConfigDownloadController(TenantService tenants, CustomPropertiesHolder properties, Path directory) {
        this.tenants = tenants; this.properties = properties; this.directory = directory;
    }

    /** Resolves the registered filename inside allTenants and returns a non-cacheable attachment. */
    @GetMapping("/tenants/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> download(@RequestParam String tenant) throws IOException {
        Path file = resolve(tenants, properties, directory, tenant);
        String name = file.getFileName().toString();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.TEXT_PLAIN).body(Files.readAllBytes(file));
    }
    /** Accepts only a registered filename inside the configuration directory, including symlink checks. */
    static Path resolve(TenantService tenants, CustomPropertiesHolder properties, Path directory, String tenant) throws IOException {
        if (!tenants.getAllTenants().containsKey(tenant)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var custom = properties.getCustomProperties(tenant);
        String name = custom == null ? null : custom.get("propertyFileName");
        if (name == null || !Path.of(name).getFileName().toString().equals(name)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Path root = directory.toRealPath();
        Path file;
        try { file = root.resolve(name).toRealPath(); }
        catch (NoSuchFileException error) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        if (!file.startsWith(root) || !Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return file;
    }
}
