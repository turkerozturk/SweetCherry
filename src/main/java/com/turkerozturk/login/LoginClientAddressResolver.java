package com.turkerozturk.login;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Accepts a client address only from the explicitly configured reverse proxy. */
@Component
public class LoginClientAddressResolver {
    private static final String CLIENT_HEADER = "X-SweetCherry-Client-IP";
    private final String trustedProxyAddress;

    public LoginClientAddressResolver(@Value("${myapp.login.trusted-proxy-address:}") String trustedProxyAddress) {
        this.trustedProxyAddress = trustedProxyAddress.trim();
    }

    /** Falls back to the TCP peer for direct, missing, or malformed proxy requests. */
    public String resolve(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (trustedProxyAddress.isEmpty() || !trustedProxyAddress.equals(peer)) return peer;
        Enumeration<String> headers = request.getHeaders(CLIENT_HEADER);
        if (!headers.hasMoreElements()) return peer;
        String first = headers.nextElement();
        if (headers.hasMoreElements()) return peer;
        String client = parseLiteralAddress(first);
        return client == null ? peer : client;
    }

    /** Rejects hostnames, address lists, ports, and untrusted text without DNS lookups. */
    private String parseLiteralAddress(String value) {
        if (value == null || value.length() > 45 || !value.equals(value.trim())) return null;
        if (value.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) {
            String[] parts = value.split("\\.");
            int[] bytes = new int[4];
            for (int i = 0; i < 4; i++) {
                bytes[i] = Integer.parseInt(parts[i]);
                if (bytes[i] > 255) return null;
            }
            return bytes[0] + "." + bytes[1] + "." + bytes[2] + "." + bytes[3];
        }
        if (value.contains(":") && value.matches("[0-9a-fA-F:]+")) {
            try {
                InetAddress parsed = InetAddress.getByName(value);
                if (parsed instanceof Inet6Address) return parsed.getHostAddress();
            } catch (UnknownHostException ignored) {
                // Numeric IPv6 input was malformed.
            }
        }
        return null;
    }
}
