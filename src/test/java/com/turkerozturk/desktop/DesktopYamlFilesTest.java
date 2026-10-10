package com.turkerozturk.desktop;
import com.turkerozturk.desktop.config.DesktopYamlFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class DesktopYamlFilesTest {
 @TempDir Path directory;
 private DesktopYamlFiles files(){return new DesktopYamlFiles(directory,getClass().getClassLoader());}
 private String changed(String yaml,String key,String type,String value){var field=DesktopYamlFiles.fields(yaml).stream().filter(f->f.key().equals(key)).findFirst().orElseThrow();return DesktopYamlFiles.update(yaml,Map.of(field.id(),new DesktopYamlFiles.Field(field.id(),field.key(),type,value,field.secret())));}
 @Test void missingFileUsesBundledDefaultAndCreatesOnlyAfterChange()throws Exception {
  var service=files();var snapshot=service.read(new DesktopYamlFiles.Choice("application.yml",false));
  assertThat(service.save(snapshot,snapshot.yaml())).isFalse();assertThat(directory.resolve("application.yml")).doesNotExist();
  String edited=changed(snapshot.yaml(),"myapp.debug","boolean","true");assertThat(service.save(snapshot,edited)).isTrue();assertThat(Files.readString(directory.resolve("application.yml"))).isEqualTo(edited);
 }
 @Test void editsNestedAndSequenceValuesWithoutTouchingCommentsSecretsOrUnicode(){
  String yaml="# 🍒 Türkçe\nserver:\n  port: 8080 # retain\n  ssl:\n    password: 'super-secret'\nitems:\n  - name: first\n    enabled: false\n  - name: second\n";
  String edited=changed(yaml,"server.port","number","8443");edited=changed(edited,"items[0].enabled","boolean","true");
  assertThat(edited).contains("# 🍒 Türkçe","port: 8443 # retain","password: 'super-secret'","enabled: true","name: second");
  assertThat(DesktopYamlFiles.fields(edited).stream().filter(f->f.key().equals("server.ssl.password")).findFirst().orElseThrow().secret()).isTrue();
 }
 @Test void blockScalarAndMultiDocumentUpdatesStayValid(){
  String yaml="text: |\n  first\n  second\nnext: 1\n---\nmyapp:\n  debug: false\n";
  String edited=changed(yaml,"text","string","new\nvalue\n");edited=changed(edited,"[2] myapp.debug","boolean","true");
  assertThat(DesktopYamlFiles.fields(edited).get(0).value()).isEqualTo("new\nvalue\n");assertThat(edited).contains("next: 1","debug: true");
 }
 @Test void staleExternalEditAndNewFileConflictAreRejected()throws Exception {
  var service=files();var defaults=service.read(new DesktopYamlFiles.Choice("application.yml",false));Files.writeString(directory.resolve("application.yml"),"myapp: {debug: false}\n");
  assertThatThrownBy(()->service.save(defaults,changed(defaults.yaml(),"myapp.debug","boolean","true"))).isInstanceOf(java.io.IOException.class);
  var existing=service.read(new DesktopYamlFiles.Choice("application.yml",true));Files.writeString(directory.resolve("application.yml"),"# changed\nmyapp: {debug: false}\n");
  assertThatThrownBy(()->service.save(existing,changed(existing.yaml(),"myapp.debug","boolean","true"))).isInstanceOf(java.io.IOException.class);
  assertThat(Files.readString(directory.resolve("application.yml"))).startsWith("# changed");
 }
 @Test void listsRootAndConfigYamlWithoutRecursingOrSelectingOtherFiles()throws Exception {
  Files.createDirectory(directory.resolve("config"));Files.writeString(directory.resolve("custom.yaml"),"value: true\n");Files.writeString(directory.resolve("config/application-local.yml"),"value: true\n");Files.writeString(directory.resolve("secrets.txt"),"ignored");
  assertThat(files().choices().stream().map(DesktopYamlFiles.Choice::relative)).contains("custom.yaml","config/application-local.yml","application-operations.yml").doesNotContain("secrets.txt");
  assertThatThrownBy(()->files().read(new DesktopYamlFiles.Choice("../application.yml",true))).isInstanceOf(java.io.IOException.class);
 }
 @Test void rejectsInvalidYamlAliasesTagsAndWrongScalarTypeBeforeWrite()throws Exception {
  for(String yaml:List.of("key: 1\nkey: 2\n","a: &anchor [1]\nb: *anchor\n","bad: !!java.lang.Runtime {}\n","items: [unclosed"))assertThatThrownBy(()->DesktopYamlFiles.validate(yaml)).isInstanceOf(RuntimeException.class);
  assertThatThrownBy(()->changed("key: true\n","key","boolean","perhaps")).isInstanceOf(IllegalArgumentException.class);
  var service=files();var snapshot=service.read(new DesktopYamlFiles.Choice("application.yml",false));assertThatThrownBy(()->service.save(snapshot,"bad: [")).isInstanceOf(RuntimeException.class);assertThat(directory.resolve("application.yml")).doesNotExist();
 }
 @Test void rawYamlSupportsAddingAndRemovingSettings()throws Exception {
  Path file=directory.resolve("custom.yaml");Files.writeString(file,"first: 1\n");var service=files();var snapshot=service.read(new DesktopYamlFiles.Choice("custom.yaml",true));
  service.save(snapshot,"# manual edit\nsecond: [one, two]\n");assertThat(Files.readString(file)).isEqualTo("# manual edit\nsecond: [one, two]\n");
 }
}
