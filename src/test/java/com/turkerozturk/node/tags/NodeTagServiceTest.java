package com.turkerozturk.node.tags;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class NodeTagServiceTest {
 private Connection connection;
 @BeforeEach void setup()throws Exception{
  connection=DriverManager.getConnection("jdbc:sqlite::memory:");
  try(var sql=connection.createStatement()){
   sql.execute("CREATE TABLE node(node_id INTEGER,name TEXT,tags TEXT COLLATE NOCASE,is_ro INTEGER,is_richtxt INTEGER,syntax TEXT)");
   sql.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,master_id INTEGER)");
   sql.execute("INSERT INTO node VALUES(1,'<one>','foo foobar Türkçe foo',0,3,'plain-text'),(2,'Two','bar foo',0,0,'plain-text'),(3,'Parent',NULL,0,0,'plain-text'),(4,'Four','Foo C++',0,0,'plain-text'),(5,'Orphan','orphan-tag',0,0,'plain-text'),(6,'Empty','  ',0,0,'plain-text')");
   sql.execute("INSERT INTO children VALUES(1,0,0),(2,3,0),(3,0,0),(4,0,0),(6,0,0),(11,3,1),(99,0,999)");
  }
 }
 @AfterEach void close()throws Exception{connection.close();}
 private NodeTagService.View view(List<String> tags,String mode,int limit,int page)throws Exception{return NodeTagService.inspect(connection,new NodeTagService.Options(tags,mode,limit,page));}
 @Test void catalogueSplitsAndDeduplicatesWithoutSqlCollationLoss()throws Exception{
  assertThat(NodeTagService.catalogue(connection)).containsExactly("bar","C++","Foo","foo","foobar","orphan-tag","Türkçe");
 }
 @Test void matchesWholeExactTagsAndKeepsSharedOccurrenceAndMasterProperties()throws Exception{
  var any=view(List.of("foo"),"any",50,1);assertThat(any.matched()).isEqualTo(3);
  assertThat(any.rows().stream().map(NodeTagService.Row::id)).containsExactly(1L,11L,2L);
  var shared=any.rows().get(1);assertThat(shared.contentId()).isEqualTo(1);assertThat(shared.shared()).isTrue();assertThat(shared.propertiesAvailable()).isTrue();
  assertThat(shared.breadcrumb()).contains("Parent [3]","<one> [11]");
  assertThat(view(List.of("foo","bar"),"all",50,1).rows().stream().map(NodeTagService.Row::id)).containsExactly(2L);
  assertThat(view(List.of("Foo"),"any",50,1).rows().stream().map(NodeTagService.Row::id)).containsExactly(4L);
  assertThat(view(List.of("fo"),"any",50,1).matched()).isZero();
 }
 @Test void noSelectionShowsTaggedLocationsAndPaginationKeepsGlobalStats()throws Exception{
  var result=view(List.of(),"any",2,2);assertThat(result.taggedContents()).isEqualTo(4);assertThat(result.taggedPlaces()).isEqualTo(4);
  assertThat(result.matched()).isEqualTo(4);assertThat(result.rows()).hasSize(2);assertThat(result.pages()).isEqualTo(2);assertThat(result.first()).isEqualTo(3);
 }
 @Test void missingRealMasterAndCyclesDoNotYieldBrokenPropertiesLinksOrLoop()throws Exception{
  try(var sql=connection.createStatement()){sql.execute("DELETE FROM children WHERE node_id=1");sql.execute("UPDATE children SET father_id=11 WHERE node_id=3");}
  var result=view(List.of("foo"),"any",50,1);var shared=result.rows().stream().filter(row->row.id()==11).findFirst().orElseThrow();
  assertThat(shared.propertiesAvailable()).isFalse();assertThat(shared.breadcrumb()).contains("↻");
 }
 @Test void metadataOnlyReadDoesNotNeedTextOrObjectTables()throws Exception{
  // The fixture intentionally has neither txt nor any BLOB/object tables.
  assertThat(view(List.of(),"any",50,1).rows()).hasSize(4);
  try(var sql=connection.createStatement();var rows=sql.executeQuery("SELECT tags FROM node WHERE node_id=1")){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("foo foobar Türkçe foo");}
 }
}
