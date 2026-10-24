package com.turkerozturk.multipledatabases;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

/** Only bundled PNG basenames are accepted; configuration never supplies an arbitrary URL/path. */
@Component
public class TenantIcons {
    public static final String DEFAULT = "ct_db";
    public static final String PROPERTY = "custom.databaseIcon";
    private static final ConcurrentHashMap<String,Boolean> EXISTS=new ConcurrentHashMap<>();
    private final CustomPropertiesHolder properties;
    public TenantIcons(CustomPropertiesHolder properties){this.properties=properties;}
    public String icon(String tenant) {
        var settings=properties.getCustomProperties(tenant);
        return resolve(settings==null?null:settings.get(PROPERTY));
    }
    public static String resolve(String value) {
        if(value==null) return DEFAULT;
        String name=value.trim();
        if(name.length()>128 || !name.matches("[A-Za-z0-9_-]+")) return DEFAULT;
        return EXISTS.computeIfAbsent(name,key->new ClassPathResource("static/img/ctbicons/"+key+".png").exists()) ? name : DEFAULT;
    }
}
