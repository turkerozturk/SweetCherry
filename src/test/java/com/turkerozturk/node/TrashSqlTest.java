package com.turkerozturk.node;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
class TrashSqlTest {
 Connection c;
 @BeforeEach void setup()throws Exception{
  c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement();var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")){
   for(String line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R"))if(line.startsWith("CREATE TABLE")||line.startsWith("INSERT INTO"))q.execute(line);
   q.execute("UPDATE node SET ts_creation=50,ts_lastsave=100");
  }c.setAutoCommit(false);
 }
 @AfterEach void close()throws Exception{c.close();}
 void sql(String s)throws Exception{try(var q=c.createStatement()){q.execute(s);}}
 long parent(long id)throws Exception{try(var q=c.prepareStatement("SELECT father_id FROM children WHERE node_id=?")){q.setLong(1,id);try(var rs=q.executeQuery()){assertThat(rs.next()).isTrue();return rs.getLong(1);}}}
 long number(String sql)throws Exception{try(var q=c.createStatement();var rs=q.executeQuery(sql)){rs.next();return rs.getLong(1);}}
 String text(String sql)throws Exception{try(var q=c.createStatement();var rs=q.executeQuery(sql)){rs.next();return rs.getString(1);}}
 TrashSql.Entry trash(long id)throws Exception{var preview=TrashSql.preview(c,id);TrashSql.trash(c,id,preview.revision(),"admin");return TrashSql.listing(c).entries().stream().filter(e->e.nodeId()==id).findFirst().orElseThrow();}
 @Test void emptyPreviewDoesNotCreateAnyNodesAndNameCollisionCannotOverwriteUserNotes()throws Exception{
  long count=number("SELECT COUNT(*) FROM node");assertThat(TrashSql.listing(c).entries()).isEmpty();TrashSql.preview(c,2);assertThat(number("SELECT COUNT(*) FROM node")).isEqualTo(count);
  sql("UPDATE node SET name='TRASH BIN' WHERE node_id=1");assertThatThrownBy(()->trash(2)).hasMessageContaining("trash.nameCollision");assertThat(parent(2)).isZero();
 }
 @Test void roundTripKeepsIdsPayloadsLinksBookmarksAndOriginalSiblingPosition()throws Exception{
  String before=text("SELECT txt FROM node WHERE node_id=2");long imageCount=number("SELECT COUNT(*) FROM image"),bookmarks=number("SELECT COUNT(*) FROM bookmark");
  var e=trash(2);assertThat(parent(2)).isEqualTo(e.id());assertThat(parent(3)).isEqualTo(2);assertThat(number("SELECT ts_creation FROM node WHERE node_id=2")).isEqualTo(50);assertThat(number("SELECT ts_lastsave FROM node WHERE node_id=2")).isGreaterThan(100);assertThat(number("SELECT ts_lastsave FROM node WHERE node_id=3")).isEqualTo(100);
  assertThat(text("SELECT txt FROM node WHERE node_id=2")).isEqualTo(before);assertThat(number("SELECT COUNT(*) FROM image")).isEqualTo(imageCount);assertThat(number("SELECT COUNT(*) FROM bookmark")).isEqualTo(bookmarks);
  assertThat(e.originalAvailable()).isTrue();assertThat(e.actor()).isEqualTo("admin");assertThat(e.externalSharedLocations()).isPositive();
  assertThat(TrashSql.restore(c,e.id(),e.revision(),false)).isEqualTo(2);assertThat(parent(2)).isZero();assertThat(number("SELECT sequence FROM children WHERE node_id=2")).isEqualTo(2);assertThat(parent(3)).isEqualTo(2);assertThat(TrashSql.listing(c).entries()).isEmpty();assertThat(number("SELECT COUNT(*) FROM node WHERE node_id="+e.id())).isZero();
 }
 @Test void restoringASharedOccurrencePreservesItsMasterAndLiveEdits()throws Exception{
  var e=trash(11);assertThat(number("SELECT master_id FROM children WHERE node_id=11")).isEqualTo(2);
  sql("UPDATE node SET name='Edited outside',txt='Live shared content',ts_lastsave=200 WHERE node_id=2");
  var current=TrashSql.entry(c,e.id());assertThat(current.name()).isEqualTo("Edited outside");assertThatThrownBy(()->TrashSql.restore(c,e.id(),e.revision(),false)).hasMessageContaining("trash.changed");
  TrashSql.restore(c,e.id(),current.revision(),false);assertThat(number("SELECT master_id FROM children WHERE node_id=11")).isEqualTo(2);assertThat(text("SELECT txt FROM node WHERE node_id=2")).isEqualTo("Live shared content");assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=11")).isZero();
 }
 @Test void missingOrTrashedParentRequiresExplicitRootChoice()throws Exception{
  var e=trash(3);trash(2);var current=TrashSql.entry(c,e.id());assertThat(current.originalAvailable()).isFalse();assertThatThrownBy(()->TrashSql.restore(c,e.id(),current.revision(),false)).hasMessageContaining("trash.parentMissing");
  TrashSql.restore(c,e.id(),current.revision(),true);assertThat(parent(3)).isZero();
 }
 @Test void staleTreeAndMalformedRecoveryMetadataNeverMoveOrDeletePayloads()throws Exception{
  var preview=TrashSql.preview(c,2);sql("UPDATE children SET sequence=99 WHERE node_id=4");assertThatThrownBy(()->TrashSql.trash(c,2,preview.revision(),"admin")).hasMessageContaining("trash.changed");assertThat(parent(2)).isZero();
  var e=trash(2);sql("UPDATE children SET sequence=98 WHERE node_id=4");assertThatThrownBy(()->TrashSql.purge(c,e.id(),e.revision())).hasMessageContaining("trash.changed");assertThat(parent(2)).isEqualTo(e.id());
  sql("UPDATE node SET txt='"+TrashSql.PREFIX+"broken JSON' WHERE node_id="+e.id());assertThatThrownBy(()->TrashSql.listing(c)).hasMessageContaining("trash.changed");assertThat(parent(2)).isEqualTo(e.id());
 }
 @Test void ordinaryDeleteMoveCopyAndMetadataPathsCannotBypassRecoveryContainers()throws Exception{
  var e=trash(2);long root=TrashSql.listing(c).rootId();
  for(long id:List.of(root,e.id(),2L,3L)){
   assertThatThrownBy(()->TrashSql.guardDelete(c,id)).hasMessageContaining("trash.usePage");assertThatThrownBy(()->TrashSql.guardMove(c,List.of(id),0)).hasMessageContaining("trash.usePage");
  }
  assertThatThrownBy(()->TrashSql.guardMove(c,List.of(1L),root)).hasMessageContaining("trash.usePage");assertThatThrownBy(()->TrashSql.guardHelper(c,e.id())).hasMessageContaining("trash.usePage");
  TrashSql.guardMove(c,List.of(1L),4);TrashSql.guardDelete(c,1);
 }
 @Test void permanentDeleteUsesExistingSharedSurvivorRules()throws Exception{
  var e=trash(2);long outside=number("SELECT COUNT(*) FROM children WHERE master_id=2");assertThat(outside).isPositive();
  TrashSql.purge(c,e.id(),TrashSql.purgePreview(c,e.id()).revision());assertThat(TrashSql.listing(c).entries()).isEmpty();assertThat(number("SELECT COUNT(*) FROM children WHERE node_id IN (2,3)")).isZero();
  assertThat(number("SELECT COUNT(*) FROM node WHERE node_id=11")).isEqualTo(1);assertThat(number("SELECT master_id FROM children WHERE node_id=11")).isZero();
 }
 @Test void secondDeletionReusesTheRootAndUnsafeHelperChangesFailClosed()throws Exception{
  var first=trash(1);long root=TrashSql.listing(c).rootId();trash(2);assertThat(TrashSql.listing(c).rootId()).isEqualTo(root);assertThat(TrashSql.listing(c).entries()).hasSize(2);
  sql("UPDATE children SET father_id=4 WHERE node_id="+root);assertThatThrownBy(()->TrashSql.restore(c,first.id(),first.revision(),true)).hasMessageContaining("trash.changed");
 }
 @Test void permanentDeletionRejectsSameSecondTextAndBinaryChanges()throws Exception{
  sql("INSERT INTO image VALUES(2,0,'left','',X'0102FF','file.bin','',0)");var e=trash(2);String revision=TrashSql.purgePreview(c,e.id()).revision();
  sql("UPDATE image SET png=X'0102FE' WHERE node_id=2");assertThatThrownBy(()->TrashSql.purge(c,e.id(),revision)).hasMessageContaining("trash.changed");
  String next=TrashSql.purgePreview(c,e.id()).revision();sql("UPDATE node SET txt='changed without timestamp' WHERE node_id=2");assertThatThrownBy(()->TrashSql.purge(c,e.id(),next)).hasMessageContaining("trash.changed");assertThat(parent(2)).isEqualTo(e.id());
 }

}
