package com.turkerozturk.settings;
import com.turkerozturk.settings.editing.ApplicationSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class ApplicationSettingsTest {
 @TempDir Path directory;
 private Map<String,String> values(){Map<String,String> values=new LinkedHashMap<>();ApplicationSettingsService.SETTINGS.forEach(s->values.put(s.key(),s.fallback()));return values;}
 @Test void preservesCommentsSecretsAndUnicodeWhileUpdatingAllowedScalars(){
  String input="# 🍒 Türkçe\nastronomy:\n  enabled: false # retain\n  latitude: 40.9\nserver:\n  port: 443\n  ssl:\n    key-store-password: 'SECRET' # keep\nmyapp:\n  debug: false\n";
  var v=values();v.put("myapp.debug","true");String result=ApplicationSettingsService.update(input,v);
  assertThat(result).contains("# 🍒 Türkçe","enabled: true # retain","port: 443","key-store-password: 'SECRET' # keep","debug: true","myapp.pdf.max-nodes: 512");
  assertThat(ApplicationSettingsService.update(result,v)).isEqualTo(result);
 }
 @Test void rejectsInvalidLimitsCoordinatesAndUnlistedKeys(){
  for(String key:List.of("myapp.pdf.max-nodes","myapp.pdf.max-depth","astronomy.latitude","astronomy.timezone")){
   var v=values();v.put(key,key.equals("astronomy.timezone")?"invalid/zone":"9999999999");assertThatThrownBy(()->ApplicationSettingsService.update("myapp: {}\n",v)).isInstanceOf(RuntimeException.class);
  }
  var v=values();v.put("server.port","80");assertThatThrownBy(()->ApplicationSettingsService.update("myapp: {}\n",v)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void rejectsAmbiguousYaml(){for(String yaml:List.of("myapp:\n  debug: false\n  debug: true\n","myapp: &a {debug: false}\nother: *a\n","myapp: {}\n---\nmyapp: {}\n"))assertThatThrownBy(()->ApplicationSettingsService.update(yaml,values())).isInstanceOf(RuntimeException.class);}
 @Test void editsActualLoadedExternalFileAndRejectsStaleForm()throws Exception {
  Path file=directory.resolve("application.yml");Files.writeString(file,"# settings\nmyapp:\n  debug: false\nserver:\n  port: 443\n");
  var environment=new StandardEnvironment();new YamlPropertySourceLoader().load("test",new FileSystemResource(file)).forEach(p->environment.getPropertySources().addLast(p));
  var service=new ApplicationSettingsService(environment);var snapshot=service.read();var v=values();v.put("myapp.debug","true");service.save(snapshot.revision(),v);
  assertThat(Files.readString(file)).contains("debug: true","port: 443","# settings");
  assertThatThrownBy(()->service.save(snapshot.revision(),v)).isInstanceOf(IllegalArgumentException.class);
  assertThat(service.read().effective().get("myapp.debug")).isEqualTo("false");
 }
 @Test void excludesClasspathYamlEvenWhenBootResolvesItToAFile()throws Exception {
  Path file=directory.resolve("application.yml");Files.writeString(file,"myapp: {debug: false}\n");
  var environment=new StandardEnvironment();new YamlPropertySourceLoader().load("Config resource via optional:classpath:/",new FileSystemResource(file)).forEach(p->environment.getPropertySources().addLast(p));
  assertThatThrownBy(()->new ApplicationSettingsService(environment).file()).isInstanceOf(java.io.IOException.class);
 }
 @Test void cannotCreateOrEditUnloadedConfig(){var service=new ApplicationSettingsService(new StandardEnvironment());assertThatThrownBy(service::read).isInstanceOf(java.io.IOException.class);}
}
