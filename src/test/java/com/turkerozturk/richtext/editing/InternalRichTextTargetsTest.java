package com.turkerozturk.richtext.editing;

import java.sql.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class InternalRichTextTargetsTest {
 private Connection connection;
 @BeforeEach void setup() throws Exception {
  connection=DriverManager.getConnection("jdbc:sqlite::memory:");
  try(var sql=connection.createStatement()) {
   sql.execute("CREATE TABLE node(node_id INTEGER,name TEXT COLLATE NOCASE,is_ro INTEGER,is_richtxt INTEGER,syntax TEXT)");
   sql.execute("CREATE TABLE children(node_id INTEGER,master_id INTEGER)");
   sql.execute("CREATE TABLE image(node_id INTEGER,anchor TEXT)");
   sql.execute("INSERT INTO node VALUES(2,'Türkçe <Name>',0,3,'plain-text'),(3,'same',0,0,'plain-text'),(4,'SAME',0,0,'plain-text'),(5,'same',0,0,'plain-text')");
   sql.execute("INSERT INTO children VALUES(2,0),(11,2),(3,0),(4,0),(5,0),(99,999)");
   sql.execute("INSERT INTO image VALUES(2,'çapa & bölüm'),(2,'çapa & bölüm'),(2,''),(3,'other')");
  }
 }
 @AfterEach void close() throws Exception {connection.close();}
 @Test void resolvesSharedOccurrenceWithoutLosingItsIdOrReadingContent() throws Exception {
  var result=InternalRichTextTargets.read(connection,11,true);
  assertThat(result.nodeId()).isEqualTo("11");assertThat(result.name()).isEqualTo("Türkçe <Name>");
  assertThat(result.bold()).isTrue();assertThat(result.anchors()).containsExactly("çapa & bölüm");
  assertThat(InternalRichTextTargets.read(connection,99,true)).isNull();
 }
 @Test void findsAllExactNamesWithoutCollationCaseFolding() throws Exception {
  var result=InternalRichTextTargets.matches(connection,"same");
  assertThat(result.targets().stream().map(InternalRichTextTargets.Target::nodeId)).containsExactly("3","5");
  assertThat(result.truncated()).isFalse();assertThat(InternalRichTextTargets.matches(connection,"Türkçe <Name>").targets()).hasSize(2);
 }
 @Test void validatesExistingOccurrenceAndAnchorRejectingMissingMalformedAndOverflowTargets() throws Exception {
  InternalRichTextTargets.validate(connection,"node 11");InternalRichTextTargets.validate(connection,"node 11 çapa & bölüm");
  for(String link:java.util.List.of("node 99","node 0","node -2","node 9223372036854775808","node 11 missing","node 11 ","node 11 a\n"))
   assertThatThrownBy(()->InternalRichTextTargets.validate(connection,link)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void boundsDuplicateNameResultsAndSignalsTruncation() throws Exception {
  try(var sql=connection.createStatement()){for(int i=100;i<301;i++)sql.execute("INSERT INTO children VALUES("+i+",2)");}
  var result=InternalRichTextTargets.matches(connection,"Türkçe <Name>");assertThat(result.targets()).hasSize(200);assertThat(result.truncated()).isTrue();
 }
}
