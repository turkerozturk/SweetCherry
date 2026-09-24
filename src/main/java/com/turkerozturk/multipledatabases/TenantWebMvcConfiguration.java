package com.turkerozturk.multipledatabases;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantWebMvcConfiguration implements WebMvcConfigurer {

    private final TenantRequiredInterceptor tenantRequiredInterceptor;

    public TenantWebMvcConfiguration(TenantRequiredInterceptor tenantRequiredInterceptor) {
        this.tenantRequiredInterceptor = tenantRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantRequiredInterceptor);
    }
}
