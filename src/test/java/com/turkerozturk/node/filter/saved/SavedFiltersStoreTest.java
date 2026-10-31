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
}
