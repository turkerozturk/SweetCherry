package com.turkerozturk.richtext.editing;
import com.turkerozturk.richtext.experimental.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class RichTextWikiLinksTest {
 Connection c;
 @BeforeEach void setup()throws Exception{c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){
  q.execute("CREATE TABLE node(node_id INTEGER,name TEXT)");q.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,master_id INTEGER)");
  q.execute("INSERT INTO node VALUES(1,'Unique'),(2,'Duplicate'),(3,'Duplicate'),(4,'Branch A'),(5,'Branch B')");q.execute("INSERT INTO children VALUES(1,0,0),(2,4,0),(3,5,0),(4,0,0),(5,0,0),(11,5,2)");
 }}
 @AfterEach void close()throws Exception{c.close();}
 RichTextDocument doc(String text){return new RichTextDocument(List.of(new RichTextDocument.TextRun(text,Map.of(),0)));}
 RichTextWikiLinks.Result resolve(String text,Map<String,String> choices,boolean keep)throws Exception{return RichTextWikiLinks.resolve(c,doc(text),choices,keep,false);}
 String text(RichTextDocument d){return d.runs().stream().map(RichTextDocument.TextRun::text).collect(java.util.stream.Collectors.joining());}
 @Test void uniqueNamesAndAliasesBecomeRealLinksWithUnicodeOffsets()throws Exception{
  var result=resolve("😀 [[Unique]] / [[ Unique |my label]]",Map.of(),false);
  assertThat(text(result.document())).isEqualTo("😀 Unique / my label");assertThat(result.document().runs()).filteredOn(r->r.attributes().containsKey("link")).allMatch(r->r.attributes().get("link").equals("node 1"));
  assertThat(new RichTextXmlReader().read(new RichTextXmlWriter().write(result.document()))).isEqualTo(result.document());
 }
 @Test void expressionsAcrossFormattingRunsKeepLabelFormattingAndMoveObjectOffsets()throws Exception{
  var input=new RichTextDocument(List.of(new RichTextDocument.TextRun("😀 [[Uni",Map.of("weight","heavy"),0),new RichTextDocument.TextRun("que]]",Map.of("style","italic"),7),new RichTextDocument.TextRun("\uFFFC",Map.of("__sweet_object","codebox:12"),12)));
  var result=RichTextWikiLinks.resolve(c,input,Map.of(),false,false);
  assertThat(text(result.document())).isEqualTo("😀 Unique\uFFFC");assertThat(result.document().runs()).anyMatch(r->r.text().equals("Uni")&&r.attributes().get("weight").equals("heavy")&&r.attributes().get("link").equals("node 1"));
  var ref=new ProtectedRichTextCodec.Reference("codebox",12);var saved=new ProtectedRichTextCodec().save(result.document(),List.of(ref));assertThat(saved.offsets().get(ref)).isEqualTo(8);
 }
 @Test void duplicateNamesRequireSelectionAndExposeBreadcrumbsAndSharedOccurrences()throws Exception{
  var preview=RichTextWikiLinks.resolve(c,doc("[[Duplicate]]"),Map.of(),true,true).preview();
  assertThat(preview.issues()).hasSize(1);assertThat(preview.issues().get(0).candidates()).extracting(RichTextWikiLinks.Candidate::breadcrumb).contains("Branch A [4] / Duplicate [2]","Branch B [5] / Duplicate [3]","Branch B [5] / Duplicate [11]");
  assertThat(preview.issues().get(0).candidates()).anyMatch(x->x.id().equals("11")&&x.shared());
  assertThatThrownBy(()->resolve("[[Duplicate]]",Map.of(),false)).hasMessageContaining("wikiUnresolved");
  assertThat(resolve("[[Duplicate]]",Map.of("Duplicate","3"),false).document().runs().get(0).attributes()).containsEntry("link","node 3");
 }
 @Test void missingNamesRemainCompleteExpressionsOnlyWithExplicitAcknowledgement()throws Exception{
  assertThatThrownBy(()->resolve("[[Missing|label]]",Map.of(),false)).hasMessageContaining("wikiUnresolved");
  var kept=resolve("[[Missing|label]] [[Unique]]",Map.of("Missing","0","Unique","1"),true);assertThat(text(kept.document())).isEqualTo("[[Missing|label]] Unique");
  assertThatThrownBy(()->resolve("[[Missing]]",Map.of("Missing","0"),false)).hasMessageContaining("wikiUnresolved");
 }
 @Test void renamedTargetsDoNotSilentlySwitchToAnotherUniqueNode()throws Exception{
  try(var q=c.createStatement()){q.execute("UPDATE node SET name='Renamed' WHERE node_id=1");q.execute("INSERT INTO node VALUES(6,'Unique')");q.execute("INSERT INTO children VALUES(6,0,0)");}
  assertThatThrownBy(()->resolve("[[Unique]]",Map.of("Unique","1"),false)).hasMessageContaining("wikiTargetChanged");
 }
 @Test void escapeExistingLinksAndProtectedSlotsAreNotReinterpreted()throws Exception{
  String literal="\\[[Unique]] [[[Unique]]] [[Missing\nName]]";assertThat(text(resolve(literal,Map.of(),false).document())).isEqualTo(literal);
  var input=new RichTextDocument(List.of(new RichTextDocument.TextRun("[[Unique]]",Map.of("link","webs https://example.org"),0),new RichTextDocument.TextRun("[[Unique]]",Map.of("__sweet_object","codebox:10"),10)));
  assertThat(RichTextWikiLinks.resolve(c,input,Map.of(),false,false).document()).isEqualTo(input);
 }
 @Test void malformedChoicesAndExcessiveExpressionsAreRejected(){
  for(String json:List.of("[]","{\"Unique\":1}","{\"Unique\":\"-1\"}","{\"Unique\":\"node 1\"}"))assertThatThrownBy(()->RichTextWikiLinks.decode(json)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->resolve("[[Unique]]",Map.of("Other","1"),false)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->resolve("[[Unique]] ".repeat(201),Map.of(),false)).hasMessageContaining("wikiLimit");
 }
}
