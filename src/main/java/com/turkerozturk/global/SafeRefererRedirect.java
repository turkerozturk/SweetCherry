package com.turkerozturk.global;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

/** Keep navigation local even when the Referer header is missing or forged. */
public final class SafeRefererRedirect {
    private SafeRefererRedirect() { }

    public static String target(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null) {
                String scheme = uri.getScheme();
                if (scheme == null) {
                    return "/";
                }
                int port = uri.getPort() >= 0 ? uri.getPort()
                        : "https".equalsIgnoreCase(scheme) ? 443 : 80;
                if (!uri.getHost().equalsIgnoreCase(request.getServerName())
                        || port != request.getServerPort()
                        || !scheme.equalsIgnoreCase(request.getScheme())
                        || uri.getUserInfo() != null) {
                    return "/";
                }
            } else if (uri.getScheme() != null || uri.getRawAuthority() != null) {
                return "/";
            }
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return "/";
            }
            return path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (IllegalArgumentException exception) {
            return "/";
        }
    }
}
