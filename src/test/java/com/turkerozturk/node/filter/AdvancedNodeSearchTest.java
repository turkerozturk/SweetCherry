package com.turkerozturk.node.filter;

import com.turkerozturk.helpers.NodeIcon;
import org.junit.jupiter.api.*;
import java.sql.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AdvancedNodeSearchTest {
 private Connection connection;
 @BeforeEach void setup()throws Exception{
  connection=DriverManager.getConnection("jdbc:sqlite::memory:");try(var s=connection.createStatement()){
   s.execute("CREATE TABLE node(node_id INTEGER,name TEXT,txt TEXT,tags TEXT,syntax TEXT,is_ro INTEGER,is_richtxt INTEGER,ts_creation INTEGER,ts_lastsave INTEGER)");
   s.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,master_id INTEGER)");
   s.execute("INSERT INTO node VALUES(1,'Raspberry notes','<node><rich_text>backup & raw</rich_text></node>','work java','custom-colors',0,3,100,500),(2,'Backup plans','raspberry tips 100% _file!','home javascript','plain-text',1,0,200,400),(3,'Other','empty','work','markdown',0,0,300,400),(4,'Root','',NULL,'plain-text',0,0,50,50),(5,'Future note','hello','future','plain-text',0,0,4102444800,600)");
   s.execute("INSERT INTO children VALUES(4,0,0),(1,4,0),(2,4,0),(3,1,0),(10,2,1),(5,0,0)");
  }
 }
 @AfterEach void close()throws Exception{connection.close();}
 private List<Long> ids(FormSearch f)throws Exception{return AdvancedNodeSearch.search(connection,f).stream().map(n->n.getNodeId()).toList();}
 @Test void allWordsCanMatchAcrossTitleAndRawTextAndSharedOccurrences()throws Exception{
  var f=new FormSearch();f.setQuery("raspberry backup");assertThat(ids(f)).containsExactly(1L,10L,2L);
  f.setMatchMode("phrase");assertThat(ids(f)).isEmpty();f.setMatchMode("any");f.setQuery("raspberry empty");assertThat(ids(f)).containsExactly(1L,10L,2L,3L);
  f.setQuery("rich_text");f.setMatchMode("all");assertThat(ids(f)).containsExactly(1L,10L);
 }
 @Test void escapesWildcardsAndAllowsPhraseAndPrefixFilters()throws Exception{
  var f=new FormSearch();f.setTextFilter("100% _file!");f.setMatchMode("phrase");assertThat(ids(f)).containsExactly(2L);
  f.setTextFilter("%' OR 1=1 --");assertThat(ids(f)).isEmpty();f.setTextFilter("");f.setNameFilter("Backup");f.setNameStartsWith(true);assertThat(ids(f)).containsExactly(2L);
 }
 @Test void wholeTagsAreNotSubstringsAndSupportAnyAllAndLegacy()throws Exception{
  var f=new FormSearch();f.setTagsFilter("java");assertThat(ids(f)).containsExactly(1L,10L);
  f.setTagsFilter("work java");assertThat(ids(f)).containsExactly(1L,10L);f.setTagMode("any");f.setTagsFilter("home future");assertThat(ids(f)).containsExactly(5L,2L);
  f.setTagsFilter("java");f.setTagMode("contains");assertThat(ids(f)).containsExactly(1L,10L,2L);
 }
 @Test void paginationAndSortingAreStableAndBreadcrumbsUseOccurrenceParent()throws Exception{
  var f=new FormSearch();f.setPageSize(2);f.setSortBy("modified");f.setSortDirection("desc");assertThat(ids(f)).containsExactly(5L,1L);f.setPageNumber(1);var page=AdvancedNodeSearch.search(connection,f);assertThat(page.getTotalElements()).isEqualTo(6);assertThat(page.stream().map(n->n.getNodeId())).containsExactly(10L,2L);
  assertThat(page.getContent().get(0).getTitleColorAsHtmlHex()).matches("#[0-9A-F]{6}");
  assertThat(page.getContent().get(0).getBreadcrumbs().values()).containsExactly("Root","Backup plans","Raspberry notes");
  f.setPageNumber(999);assertThat(AdvancedNodeSearch.search(connection,f).getNumber()).isEqualTo(2);
  f.setPageSize(100);f.setPageNumber(0);f.setSortBy("name");f.setSortDirection("asc");assertThat(ids(f)).containsExactly(2L,5L,3L,1L,10L,4L);
 }
 @Test void emptyDateFiltersIncludeFutureAndInvalidRangesNeverBuildQueries()throws Exception{
  var f=new FormSearch();f.setPageSize(100000);assertThat(ids(f)).contains(5L);f.setTsCreationEndFilter(java.sql.Date.valueOf("2026-10-31"));assertThat(ids(f)).doesNotContain(5L);
  f.setSortBy("name; DROP TABLE node");assertThatThrownBy(()->AdvancedNodeSearch.plan(f,"SQLite")).isInstanceOf(IllegalArgumentException.class);
  f.setSortBy("id");f.setPageSize(0);assertThatThrownBy(()->AdvancedNodeSearch.plan(f,"SQLite")).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void iconReadOnlyBitAndIdFiltersAndCyclesWork()throws Exception{
  var f=new FormSearch();f.setNodeIcons(new LinkedHashSet<>(List.of(NodeIcon.OTHER)));assertThat(ids(f)).contains(1L,2L,10L);f.setNodeIdStartFilter(10L);assertThat(ids(f)).containsExactly(10L);
  try(var s=connection.createStatement()){s.execute("UPDATE children SET father_id=3 WHERE node_id=1");}f.setNodeIdStartFilter(1L);assertThat(AdvancedNodeSearch.search(connection,f).stream()).anyMatch(n->n.getNodeId()==1&&n.getBreadcrumbs().containsKey(-1L));
 }
 @Test void grammarUsesParametersAndPortableTagConcatenation(){var f=new FormSearch();f.setTagsFilter("a% b_");var sqlite=AdvancedNodeSearch.plan(f,"SQLite");var maria=AdvancedNodeSearch.plan(f,"MariaDB");assertThat(sqlite.where()).contains("||").doesNotContain("a%");assertThat(maria.where()).contains("CONCAT(");assertThat(maria.values()).containsExactly("% a!% %","% b!_ %");}
 @Test void validationStopsBadBoundsAndAcceptsFutureDates(){try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()){var validator=factory.getValidator();var f=new FormSearch();f.setTsCreationStartFilter(java.sql.Date.valueOf("2090-01-01"));assertThat(validator.validate(f)).isEmpty();f.setTsCreationEndFilter(java.sql.Date.valueOf("2080-01-01"));assertThat(validator.validate(f)).isNotEmpty();f.setTsCreationEndFilter(null);f.setQuery("word ".repeat(34));assertThatThrownBy(()->AdvancedNodeSearch.plan(f,"SQLite")).isInstanceOf(IllegalArgumentException.class);}}
}
