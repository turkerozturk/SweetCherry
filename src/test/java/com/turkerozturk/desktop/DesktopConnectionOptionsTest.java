package com.turkerozturk.desktop;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;
class DesktopConnectionOptionsTest {
 @TempDir Path directory;
 @Test void missingConnectionSettingsPromptButYamlAndExplicitArgumentsRemainAuthoritative()throws Exception{
  assertThat(DesktopConnectionOptions.needsPrompt(directory,new String[0])).isTrue();
  Files.writeString(directory.resolve("application.yml"),"server:\n  http:\n    port: 8090\n");
  assertThat(DesktopConnectionOptions.needsPrompt(directory,new String[0])).isFalse();
  Files.writeString(directory.resolve("application.yml"),"server.port: 9090\n");assertThat(DesktopConnectionOptions.needsPrompt(directory,new String[0])).isFalse();
  Files.writeString(directory.resolve("application.yml"),"myapp: {}\n");assertThat(DesktopConnectionOptions.needsPrompt(directory,new String[0])).isTrue();
  assertThat(DesktopConnectionOptions.needsPrompt(directory,new String[]{"--server.port=9123"})).isFalse();
  assertThat(DesktopConnectionOptions.needsPrompt(directory,new String[]{"--spring.config.location=file:custom.yml"})).isFalse();
 }
 @Test void choiceIsExplicitAndDisablesDuplicateHttpListener(){
  assertThat(DesktopConnectionOptions.arguments(new String[]{"--custom=keep"},"https",9443)).containsExactly("--custom=keep","--server.port=9443","--server.ssl.enabled=true","--server.http.enabled=false");
  assertThatThrownBy(()->DesktopConnectionOptions.arguments(new String[0],"ftp",8080)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->DesktopConnectionOptions.arguments(new String[0],"http",65536)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void configSubfolderAndYamlExtensionAreRecognizedAndBrokenYamlIsLeftToSpring()throws Exception{
  Files.createDirectory(directory.resolve("config"));Files.writeString(directory.resolve("config/application.yaml"),"server:\n  port: 8443\n  ssl:\n    enabled: true\n");
  assertThat(DesktopConnectionOptions.needsPrompt(directory,new String[0])).isFalse();
  Files.writeString(directory.resolve("config/application.yaml"),"server: [broken");assertThat(DesktopConnectionOptions.needsPrompt(directory,new String[0])).isFalse();
 }
}
