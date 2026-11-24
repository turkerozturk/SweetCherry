package com.turkerozturk.desktop;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import com.turkerozturk.startup.LocalBrowserAddress;
import static org.assertj.core.api.Assertions.*;
class LocalBrowserAddressTest {
 @Test void usesYamlHttpOrHttpsAndContextPath(){
  var env=new MockEnvironment().withProperty("server.port","9443").withProperty("server.http.port","9090");
  assertThat(LocalBrowserAddress.from(env)).isEqualTo("http://localhost:9090");
  env.withProperty("server.http.enabled","false").withProperty("server.ssl.enabled","true").withProperty("server.servlet.context-path","/notes");
  assertThat(LocalBrowserAddress.from(env)).isEqualTo("https://localhost:9443/notes");
  env.withProperty("server.ssl.enabled","false");assertThat(LocalBrowserAddress.from(env)).isEqualTo("http://localhost:9443/notes");
 }
 @Test void implicitSslConfigurationAlsoUsesHttpsUnlessExplicitlyDisabled(){
  var env=new MockEnvironment().withProperty("server.port","9443").withProperty("server.http.enabled","false").withProperty("server.ssl.key-store","classpath:keys.p12");
  assertThat(LocalBrowserAddress.from(env)).isEqualTo("https://localhost:9443");
  env.withProperty("server.ssl.enabled","false");assertThat(LocalBrowserAddress.from(env)).isEqualTo("http://localhost:9443");
 }
 @Test void matchingPortsDoNotInventAnAdditionalHttpAddressAndRandomPrimaryUsesActualPort(){
  var env=new MockEnvironment().withProperty("server.port","8443").withProperty("server.http.port","8443").withProperty("server.ssl.enabled","true");
  assertThat(LocalBrowserAddress.from(env)).isEqualTo("https://localhost:8443");
  env.withProperty("server.port","0").withProperty("local.server.port","12345").withProperty("server.http.enabled","false");
  assertThat(LocalBrowserAddress.from(env)).isEqualTo("https://localhost:12345");
 }
}
