package com.turkerozturk.node.filter;

import com.turkerozturk.node.filter.saved.*;
import jakarta.validation.Validation;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SavedSearchTreeQueryTest {
 @Test void virtualPagesReadRealAndSharedOccurrencesWithoutWritingTheDatabase()throws Exception {
  try(var connection=DriverManager.getConnection("jdbc:sqlite::memory:");var validators=Validation.buildDefaultValidatorFactory()){
   try(var sql=connection.createStatement()){
    sql.execute("CREATE TABLE node(node_id INTEGER,name TEXT,txt TEXT,tags TEXT,syntax TEXT,is_ro INTEGER,is_richtxt INTEGER,ts_creation INTEGER,ts_lastsave INTEGER)");
    sql.execute("CREATE TABLE children(node_id INTEGER,father_id INTEGER,master_id INTEGER)");
    sql.execute("INSERT INTO node VALUES(1,'Folder','','','plain-text',0,0,0,0),(2,'Task','body','todo','plain-text',0,0,0,0),(3,'Other folder','','','plain-text',0,0,0,0),(4,'Private ledger','','__sweetcherry_audit_v1__','plain-text',0,0,0,0)");
    sql.execute("INSERT INTO children VALUES(1,0,0),(2,1,0),(3,0,0),(4,0,0),(20,3,2)");
    for(int id=100;id<160;id++){sql.execute("INSERT INTO node VALUES("+id+",'Task "+id+"','body','todo','plain-text',0,0,0,0)");sql.execute("INSERT INTO children VALUES("+id+",1,0)");}
    sql.execute("PRAGMA query_only=ON");
   }
   String id="12345678-1234-1234-1234-123456789abc";var store=mock(SavedFiltersStore.class);var search=mock(AdvancedNodeSearch.class);
   when(store.read("scope")).thenReturn(new SavedFiltersStore.Snapshot("scope","revision",List.of(new SavedFiltersStore.Preset(id,"Tasks",Map.of("tagsFilter",List.of("todo"),"sortBy",List.of("id"),"sortDirection",List.of("asc"))))));
   when(search.search(any())).thenAnswer(call->AdvancedNodeSearch.search(connection,call.getArgument(0)));
   var service=new SavedSearchTreeService(store,search,validators.getValidator());
   var first=service.results("scope",id,0);assertThat(first.total()).isEqualTo(62);assertThat(first.results()).hasSize(50);assertThat(first.hasNext()).isTrue();
   assertThat(first.results().get(1).nodeId()).isEqualTo(20);assertThat(first.results().get(1).path()).isEqualTo("Other folder / Task");
   var second=service.results("scope",id,1);assertThat(second.results()).hasSize(12);assertThat(second.hasNext()).isFalse();
   assertThat(second.results()).extracting(SavedSearchTreeService.Result::nodeId).doesNotContainAnyElementsOf(first.results().stream().map(SavedSearchTreeService.Result::nodeId).toList());
  }
 }
}
