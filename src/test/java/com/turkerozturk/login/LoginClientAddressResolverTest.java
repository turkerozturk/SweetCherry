package com.turkerozturk.login;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginClientAddressResolverTest {
    private final LoginClientAddressResolver resolver = new LoginClientAddressResolver("192.168.1.20");

    @Test
    void acceptsSingleLiteralOnlyFromConfiguredProxy() {
        MockHttpServletRequest proxied = request("192.168.1.20", "203.0.113.7");
        assertEquals("203.0.113.7", resolver.resolve(proxied));
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "203.0.113.7")));
        assertEquals("192.168.1.20", new LoginClientAddressResolver("").resolve(proxied));
    }

    @Test
    void rejectsUntrustedOrAmbiguousHeaders() {
        assertEquals("192.168.1.20", resolver.resolve(request("192.168.1.20", "203.0.113.7, 127.0.0.1")));
        assertEquals("192.168.1.20", resolver.resolve(request("192.168.1.20", "example.com")));
        assertEquals("192.168.1.20", resolver.resolve(request("192.168.1.20", "999.1.2.3")));
        MockHttpServletRequest duplicated = request("192.168.1.20", "203.0.113.7");
        duplicated.addHeader("X-SweetCherry-Client-IP", "198.51.100.4");
        assertEquals("192.168.1.20", resolver.resolve(duplicated));
    }

    @Test
    void acceptsNumericIpv6AndKeepsDirectLanAddress() {
        assertEquals("2001:db8:0:0:0:0:0:1", resolver.resolve(request("192.168.1.20", "2001:db8::1")));
        assertEquals("192.168.1.55", resolver.resolve(request("192.168.1.55", "203.0.113.7")));
    }

    private MockHttpServletRequest request(String peer, String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        request.addHeader("X-SweetCherry-Client-IP", header);
        return request;
    }
}
