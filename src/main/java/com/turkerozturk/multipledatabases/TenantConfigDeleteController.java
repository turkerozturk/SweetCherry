package com.turkerozturk.multipledatabases;

import java.io.IOException;
import java.nio.file.Path;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Controller
public class TenantConfigDeleteController {
    private final TenantService tenants;
    private final CustomPropertiesHolder properties;
    private final MultitenantConfiguration configuration;
    public TenantConfigDeleteController(TenantService tenants, CustomPropertiesHolder properties, MultitenantConfiguration configuration) {
        this.tenants=tenants; this.properties=properties; this.configuration=configuration;
    }
    /** Deletes a registered connection config after confirmation; never deletes the referenced database. */
    @PostMapping("/tenants/config/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@RequestParam String tenant, HttpServletRequest request) throws IOException {
        Path file=TenantConfigDownloadController.resolve(tenants, properties,
                Path.of(MultitenantConfiguration.PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES), tenant);
        Path configuredEntry=Path.of(MultitenantConfiguration.PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES)
                .resolve(properties.getCustomProperties(tenant).get("propertyFileName"));
        if (java.nio.file.Files.isSymbolicLink(configuredEntry))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Remove symbolic-link configs manually.");
        try { configuration.deleteTenantConfig(tenant, file); }
        catch (IllegalStateException error) {
            request.getSession().setAttribute("dataSourceOperationError", "busy");
            return "redirect:/";
        }
        if (tenant.equals(request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT))) {
            request.getSession().removeAttribute(TenantContext.SESSION_VARIABLE__CURRENT_TENANT);
            request.getSession().removeAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN);
            request.getSession().removeAttribute("TENANT_POOL_GENERATION");
            TenantContext.clear();
        }
        return "redirect:/tenants";
    }
}
