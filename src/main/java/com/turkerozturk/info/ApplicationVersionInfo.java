package com.turkerozturk.info;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;

/** The running artifact is authoritative, even when external YAML still contains an old build token. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApplicationVersionInfo implements InfoContributor {
    private final ObjectProvider<BuildProperties> builds;
    public ApplicationVersionInfo(ObjectProvider<BuildProperties> builds) { this.builds=builds; }
    @Override public void contribute(Info.Builder builder) {
        var app=new LinkedHashMap<String,Object>();
        Object existing=builder.build().getDetails().get("app");
        if(existing instanceof java.util.Map<?,?> map) map.forEach((key,value)->app.put(key.toString(),value));
        var build=builds.getIfAvailable();
        String version=build==null ? ApplicationVersionInfo.class.getPackage().getImplementationVersion() : build.getVersion();
        app.putIfAbsent("name","SweetCherry");
        app.put("version",version==null ? "development" : version);
        builder.withDetail("app",app);
    }
}
