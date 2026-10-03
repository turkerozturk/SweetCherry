package com.turkerozturk.security;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Enumeration;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/** Applies HTTPS metadata before Tomcat creates session cookies, without replacing the TCP peer. */
public final class TrustedProxyHttpsValve extends ValveBase {
    public static final String PROTOCOL_HEADER = "X-SweetCherry-Forwarded-Proto";
    private final String trustedProxyAddress;
    private final int httpsPort;

    public TrustedProxyHttpsValve(String trustedProxyAddress, int httpsPort) {
        super(true);
        this.trustedProxyAddress = trustedProxyAddress.trim();
        if (this.trustedProxyAddress.isEmpty()) {
            throw new IllegalArgumentException("Proxy HTTPS requires myapp.login.trusted-proxy-address");
        }
        if (httpsPort < 1 || httpsPort > 65535) {
            throw new IllegalArgumentException("myapp.security.proxy-https-port must be between 1 and 65535");
        }
        this.httpsPort = httpsPort;
    }

    /** Accepts one exact HTTPS value only from the configured peer; native TLS is never downgraded. */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (request.isSecure() || !trustedProxyAddress.equals(request.getRemoteAddr())
                || !hasSingleHttpsHeader(request)) {
            getNext().invoke(request, response);
            return;
        }
        String originalScheme = request.getScheme();
        int originalPort = request.getServerPort();
        request.getCoyoteRequest().scheme().setString("https");
        request.setSecure(true);
        request.setServerPort(httpsPort);
        try {
            getNext().invoke(request, response);
        } finally {
            request.getCoyoteRequest().scheme().setString(originalScheme);
            request.setSecure(false);
            request.setServerPort(originalPort);
        }
    }

    /** Rejects missing, duplicate, comma-separated, or malformed protocol headers. */
    private boolean hasSingleHttpsHeader(Request request) {
        Enumeration<String> values = request.getHeaders(PROTOCOL_HEADER);
        if (!values.hasMoreElements()) return false;
        String value = values.nextElement();
        return !values.hasMoreElements() && "https".equals(value);
    }
}
