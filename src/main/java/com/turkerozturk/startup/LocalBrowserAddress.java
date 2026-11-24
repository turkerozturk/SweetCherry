package com.turkerozturk.startup;
import org.springframework.core.env.Environment;
/** Uses resolved Spring settings, including the actual primary port when it was allocated dynamically. */
public final class LocalBrowserAddress {
 private LocalBrowserAddress(){}
 public static String from(Environment environment){
  int configured=environment.getProperty("server.port",Integer.class,8080);
  int port=environment.getProperty("local.server.port",Integer.class,configured);
  int http=environment.getProperty("server.http.port",Integer.class,8080);
  boolean additional=environment.getProperty("server.http.enabled",Boolean.class,true)&&http>0&&http!=configured;
  boolean ssl=environment.getProperty("server.ssl.enabled",Boolean.class,environment.containsProperty("server.ssl.key-store")||environment.containsProperty("server.ssl.bundle")||environment.containsProperty("server.ssl.certificate"));
  String context=environment.getProperty("server.servlet.context-path","");
  return (additional?"http":ssl?"https":"http")+"://localhost:"+(additional?http:port)+(context.equals("/")?"":context);
 }
}
