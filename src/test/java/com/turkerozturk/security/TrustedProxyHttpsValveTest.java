package com.turkerozturk.security;

import com.turkerozturk.login.LoginClientAddressResolver;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrustedProxyHttpsValveTest {
    @Test
    void trustedHttpsChangesSchemePortAndSecureWithoutChangingLoginPeer() throws Exception {
        Request request = request("192.168.0.2", "https");
        request.getCoyoteRequest().getMimeHeaders().addValue("X-SweetCherry-Client-IP").setString("203.0.113.7");
        var valve = new TrustedProxyHttpsValve("192.168.0.2", 8443);
        Valve next = mock(Valve.class);
        valve.setNext(next);
        doAnswer(invocation -> {
            assertThat(request.isSecure()).isTrue();
            assertThat(request.getScheme()).isEqualTo("https");
            assertThat(request.getServerPort()).isEqualTo(8443);
            assertThat(request.getRemoteAddr()).isEqualTo("192.168.0.2");
            assertThat(new LoginClientAddressResolver("192.168.0.2").resolve(request)).isEqualTo("203.0.113.7");
            return null;
        }).when(next).invoke(eq(request), any(Response.class));
        valve.invoke(request, new Response());
        assertThat(request.isSecure()).isFalse();
        assertThat(request.getScheme()).isEqualTo("http");
        assertThat(request.getServerPort()).isEqualTo(8080);
    }

    @Test
    void directClientCannotSpoofHttpsWithAnyForwardingHeader() throws Exception {
        Request request = request("192.168.0.55", "https");
        request.getCoyoteRequest().getMimeHeaders().addValue("X-Forwarded-Proto").setString("https");
        request.getCoyoteRequest().getMimeHeaders().addValue("Forwarded").setString("proto=https");
        assertRemainsHttp(request);
    }

    @Test
    void missingDuplicateAndMalformedHeadersAreIgnored() throws Exception {
        for (String value : new String[]{null, "http", "https,http", " https", "HTTPS"}) {
            assertRemainsHttp(request("192.168.0.2", value));
        }
        Request duplicate = request("192.168.0.2", "https");
        duplicate.getCoyoteRequest().getMimeHeaders().addValue(TrustedProxyHttpsValve.PROTOCOL_HEADER).setString("https");
        assertRemainsHttp(duplicate);
    }

    @Test
    void nativeTlsIsNeverDowngradedOrAssignedProxyPort() throws Exception {
        Request request = request("192.168.0.2", "http");
        request.setSecure(true);
        request.getCoyoteRequest().scheme().setString("https");
        request.setServerPort(9443);
        var valve = new TrustedProxyHttpsValve("192.168.0.2", 443);
        valve.setNext(mock(Valve.class));
        valve.invoke(request, new Response());
        assertThat(request.isSecure()).isTrue();
        assertThat(request.getServerPort()).isEqualTo(9443);
    }

    @Test
    void restoresRequestEvenWhenDownstreamFails() throws Exception {
        Request request = request("192.168.0.2", "https");
        var valve = new TrustedProxyHttpsValve("192.168.0.2", 443);
        Valve next = mock(Valve.class);
        valve.setNext(next);
        doThrow(new jakarta.servlet.ServletException("test")).when(next).invoke(eq(request), any(Response.class));
        assertThatThrownBy(() -> valve.invoke(request, new Response())).isInstanceOf(jakarta.servlet.ServletException.class);
        assertThat(request.isSecure()).isFalse();
        assertThat(request.getScheme()).isEqualTo("http");
        assertThat(request.getServerPort()).isEqualTo(8080);
    }

    @Test
    void enabledProxyRequiresExplicitPeerAndValidPort() {
        assertThatThrownBy(() -> new TrustedProxyHttpsValve("", 443)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustedProxyHttpsValve("192.168.0.2", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrustedProxyHttpsValve("192.168.0.2", 65536)).isInstanceOf(IllegalArgumentException.class);
    }

    private void assertRemainsHttp(Request request) throws Exception {
        var valve = new TrustedProxyHttpsValve("192.168.0.2", 443);
        Valve next = mock(Valve.class);
        valve.setNext(next);
        doAnswer(invocation -> {
            assertThat(request.isSecure()).isFalse();
            assertThat(request.getScheme()).isEqualTo("http");
            assertThat(request.getServerPort()).isEqualTo(8080);
            return null;
        }).when(next).invoke(eq(request), any(Response.class));
        valve.invoke(request, new Response());
        verify(next).invoke(eq(request), any(Response.class));
    }

    private Request request(String peer, String protocol) {
        var request = new Request(new Connector());
        request.setCoyoteRequest(new org.apache.coyote.Request());
        request.getCoyoteRequest().scheme().setString("http");
        request.setServerPort(8080);
        request.setRemoteAddr(peer);
        if (protocol != null) request.getCoyoteRequest().getMimeHeaders()
                .addValue(TrustedProxyHttpsValve.PROTOCOL_HEADER).setString(protocol);
        return request;
    }
}
