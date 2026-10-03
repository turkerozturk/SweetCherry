package com.turkerozturk.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/** Enables protocol-only proxy trust explicitly; ordinary HTTP and direct SSL need no proxy settings. */
@Component
@ConditionalOnProperty(name = "myapp.security.proxy-https-enabled", havingValue = "true")
public class ProxyHttpsConfiguration implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    private final String trustedProxyAddress;
    private final int httpsPort;

    public ProxyHttpsConfiguration(
            @Value("${myapp.login.trusted-proxy-address:}") String trustedProxyAddress,
            @Value("${myapp.security.proxy-https-port:443}") int httpsPort) {
        this.trustedProxyAddress = trustedProxyAddress;
        this.httpsPort = httpsPort;
    }

    /** Adds the valve to all connectors while preserving the existing login client-IP resolver. */
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addEngineValves(new TrustedProxyHttpsValve(trustedProxyAddress, httpsPort));
    }
}
