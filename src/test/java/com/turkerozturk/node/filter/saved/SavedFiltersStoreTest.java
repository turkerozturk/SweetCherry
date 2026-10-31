package com.turkerozturk.node.filter.saved;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;import java.util.*;import static org.assertj.core.api.Assertions.*;
class SavedFiltersStoreTest {
 @TempDir Path dir; SavedFiltersStore store; final String scope="a".repeat(64);
 @BeforeEach void setup(){store=new SavedFiltersStore(new ObjectMapper(),dir.resolve("filters"));}
 Map<String,List<String>> fields(){var values=new HashMap<String,List<String>>();SavedFiltersStore.FIELDS.forEach(k->values.put(k,List.of()));values.put("textFilter",List.of("Türkçe.a.b & <text>"));values.put("nameStartsWith",List.of("true"));return values;}
 @Test void persistsLiteralValuesAndIsolatesScopes()throws Exception{var initial=store.read(scope);var saved=store.save(scope,initial.revision(),"","Test",fields());assertThat(new SavedFiltersStore(new ObjectMapper(),dir.resolve("filters")).read(scope)).isEqualTo(saved);assertThat(store.read("b".repeat(64)).presets()).isEmpty();assertThat(saved.presets().get(0).fields().get("textFilter")).containsExactly("Türkçe.a.b & <text>");}
 @Test void detectsConcurrentUpdatesAndSupportsRenameDelete()throws Exception{var initial=store.read(scope);var first=store.save(scope,initial.revision(),"","First",fields());assertThatThrownBy(()->store.save(scope,initial.revision(),"","Stale",fields())).isInstanceOf(SavedFiltersStore.Conflict.class);String id=first.presets().get(0).id();var renamed=store.save(scope,first.revision(),id,"Second",fields());assertThat(renamed.presets()).hasSize(1);assertThat(store.delete(scope,renamed.revision(),id).presets()).isEmpty();}
 @Test void rejectsUnknownFieldsTraversalAndCorruption()throws Exception{var values=fields();values.put("_csrf",List.of("secret"));assertThatThrownBy(()->store.save(scope,store.read(scope).revision(),"","Bad",values)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->store.read("../file")).isInstanceOf(IllegalArgumentException.class);Files.createDirectory(dir.resolve("filters"));Path file=dir.resolve("filters").resolve(scope+".json");Files.writeString(file,"invalid");assertThatThrownBy(()->store.save(scope,"","","Bad",fields())).isInstanceOf(java.io.IOException.class);assertThat(Files.readString(file)).isEqualTo("invalid");}
 @Test void acceptsOldPresetsWithOriginalPhraseAndTagSubstringSemantics()throws Exception{
  var old=fields();for(String key:List.of("query","matchMode","tagMode","sortBy","sortDirection"))old.remove(key);
  var saved=store.save(scope,store.read(scope).revision(),"","Legacy",old);
  assertThat(saved.presets().get(0).fields().get("matchMode")).containsExactly("phrase");assertThat(saved.presets().get(0).fields().get("tagMode")).containsExactly("contains");
  assertThat(store.read(scope)).isEqualTo(saved);
 }
 @Test void readsLegacyJsonWithoutChangingItsRevisionUntilAnExplicitSave()throws Exception{
  var old=fields();for(String key:List.of("query","matchMode","tagMode","sortBy","sortDirection"))old.remove(key);
  Path directory=dir.resolve("filters");Files.createDirectory(directory);String id=UUID.randomUUID().toString();
  var bytes=new ObjectMapper().writeValueAsBytes(Map.of("version",1,"presets",List.of(Map.of("id",id,"name","Old file","fields",old))));Path file=directory.resolve(scope+".json");Files.write(file,bytes);
  var read=store.read(scope);assertThat(read.presets().get(0).fields().get("matchMode")).containsExactly("phrase");assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
  var updated=store.save(scope,read.revision(),id,"Updated",read.presets().get(0).fields());assertThat(store.read(scope)).isEqualTo(updated);assertThat(updated.revision()).isNotEqualTo(read.revision());
 }
}
