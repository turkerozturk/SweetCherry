package com.turkerozturk.security;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import static org.assertj.core.api.Assertions.*;

/** Exercises actual Tomcat cookie creation instead of relying on a servlet request mock. */
class ProxyHttpsCookieTest {
    @Test
    void containerCreatesSecureCookiesOnlyForTrustedHttpsAndKeepsLocalHttpWorking() throws Exception {
        var factory = new TomcatServletWebServerFactory(0);
        new ProxyHttpsConfiguration("127.0.0.1", 443).customize(factory);
        var server = factory.getWebServer(context -> {
            context.getSessionCookieConfig().setHttpOnly(true);
            context.getSessionCookieConfig().setAttribute("SameSite", "Lax");
            context.addServlet("probe", new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    request.getSession();
                    response.sendRedirect(request.getScheme() + "://" + request.getServerName()
                            + (request.getServerPort() == 443 ? "" : ":" + request.getServerPort()) + "/login");
                }
            }).addMapping("/probe");
        });
        try {
            server.start();
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
            URI uri = URI.create("http://127.0.0.1:" + server.getPort() + "/probe");
            var secure = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                    .header(TrustedProxyHttpsValve.PROTOCOL_HEADER, "https").build(), HttpResponse.BodyHandlers.discarding());
            assertThat(secure.statusCode()).isEqualTo(302);
            assertThat(secure.headers().firstValue("Location").orElseThrow()).isEqualTo("https://127.0.0.1/login");
            assertThat(secure.headers().firstValue("Set-Cookie").orElseThrow())
                    .contains("Secure", "HttpOnly", "SameSite=Lax");
            var local = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.discarding());
            assertThat(local.headers().firstValue("Location").orElseThrow()).startsWith("http://127.0.0.1:");
            assertThat(local.headers().firstValue("Set-Cookie").orElseThrow())
                    .contains("HttpOnly", "SameSite=Lax").doesNotContain("Secure");
        } finally {
            server.stop();
        }
    }
}
