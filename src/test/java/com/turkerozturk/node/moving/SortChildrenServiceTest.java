package com.turkerozturk.node.moving;

import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class SortChildrenServiceTest {
    Connection c;
    @BeforeEach void setup()throws Exception{
        c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){
            q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,tags TEXT,is_ro INTEGER,ts_creation INTEGER,ts_lastsave INTEGER)");
            q.execute("CREATE TABLE children(node_id INTEGER PRIMARY KEY,father_id INTEGER,sequence INTEGER,master_id INTEGER)");
            q.execute("INSERT INTO node VALUES(1,'Parent','',0,1,10),(2,'Zulu','',0,30,50),(3,'alpha','',0,20,70),(4,'ALPHA','',0,10,60),(5,'Grandchild','',0,1,5),(6,'Other','',0,1,6)");
            q.execute("INSERT INTO children VALUES(1,0,1,0),(2,1,1,0),(3,1,2,0),(4,1,3,0),(5,2,8,0),(6,0,2,0),(7,1,4,3)");
        }
    }
    @AfterEach void close()throws Exception{c.close();}
    List<Long> order()throws Exception{var result=new ArrayList<Long>();try(var q=c.createStatement();var rs=q.executeQuery("SELECT node_id FROM children WHERE father_id=1 ORDER BY sequence,node_id")){while(rs.next())result.add(rs.getLong(1));}return result;}
    void sort(SortChildrenService.Key key,SortChildrenService.Order direction)throws Exception{var state=SortChildrenService.read(c,1,false).state();c.setAutoCommit(false);SortChildrenService.sort(c,1,key,direction,state.revision());c.commit();c.setAutoCommit(true);}
    @Test void allSixOrdersAreStableIncludingAliasSortKeysAndOnlyImmediateChildrenChange()throws Exception{
        var asc=Map.of(SortChildrenService.Key.CREATED,List.of(4L,3L,7L,2L),SortChildrenService.Key.MODIFIED,List.of(2L,4L,3L,7L),SortChildrenService.Key.NAME,List.of(3L,4L,7L,2L));
        var desc=Map.of(SortChildrenService.Key.CREATED,List.of(2L,3L,7L,4L),SortChildrenService.Key.MODIFIED,List.of(3L,7L,4L,2L),SortChildrenService.Key.NAME,List.of(2L,3L,4L,7L));
        for(var key:SortChildrenService.Key.values())for(var direction:SortChildrenService.Order.values()){
            try(var q=c.createStatement()){q.execute("UPDATE children SET sequence=CASE node_id WHEN 2 THEN 1 WHEN 3 THEN 2 WHEN 4 THEN 3 WHEN 7 THEN 4 ELSE sequence END");}
            sort(key,direction);assertThat(order()).isEqualTo((direction==SortChildrenService.Order.ASC?asc:desc).get(key));
            try(var q=c.createStatement();var rs=q.executeQuery("SELECT father_id,sequence,master_id FROM children WHERE node_id=5")){rs.next();assertThat(rs.getLong(1)).isEqualTo(2);assertThat(rs.getLong(2)).isEqualTo(8);assertThat(rs.getLong(3)).isZero();}
            try(var q=c.createStatement();var rs=q.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=3")){rs.next();assertThat(rs.getLong(1)).isEqualTo(70);}
            try(var q=c.createStatement();var rs=q.executeQuery("SELECT master_id FROM children WHERE node_id=7")){rs.next();assertThat(rs.getLong(1)).isEqualTo(3);}
        }
    }
    @Test void changedNamesDatesMembershipAndSequenceInvalidateTheConfirmation()throws Exception{
        for(String sql:List.of("UPDATE node SET name='Changed' WHERE node_id=3","UPDATE node SET ts_lastsave=71 WHERE node_id=3","UPDATE children SET father_id=6 WHERE node_id=3","UPDATE children SET sequence=99 WHERE node_id=3")){
            var state=SortChildrenService.read(c,1,false).state();c.setAutoCommit(false);try(var q=c.createStatement()){q.execute(sql);}
            assertThatThrownBy(()->SortChildrenService.sort(c,1,SortChildrenService.Key.NAME,SortChildrenService.Order.ASC,state.revision())).isInstanceOf(ResponseStatusException.class).satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));c.rollback();c.setAutoCommit(true);
        }
        assertThat(order()).containsExactly(2L,3L,4L,7L);
    }
    @Test void aliasesCanHaveTheirOwnChildOrderAndAlreadySortedOrEmptyBranchesAreNoOps()throws Exception{
        var state=SortChildrenService.read(c,7,false).state();assertThat(state.name()).isEqualTo("alpha");assertThat(state.count()).isZero();
        SortChildrenService.sort(c,7,SortChildrenService.Key.NAME,SortChildrenService.Order.ASC,state.revision());
        sort(SortChildrenService.Key.CREATED,SortChildrenService.Order.DESC);
        try(var q=c.createStatement()){q.execute("UPDATE node SET ts_lastsave=10 WHERE node_id=1");}
        sort(SortChildrenService.Key.CREATED,SortChildrenService.Order.DESC);
        try(var q=c.createStatement();var rs=q.executeQuery("SELECT ts_lastsave FROM node WHERE node_id=1")){rs.next();assertThat(rs.getLong(1)).isEqualTo(10);}
    }
    @Test void systemContainersReadOnlyParentsAndInvalidIdsAreRejected()throws Exception{
        for(String sql:List.of("UPDATE node SET is_ro=1 WHERE node_id=1","UPDATE node SET tags='__sweetcherry_trash_v1__' WHERE node_id=1","UPDATE node SET tags='__sweetcherry_audit_v1__' WHERE node_id=1","UPDATE node SET tags='__sweetcherry_reminders_v1__' WHERE node_id=1")){
            c.setAutoCommit(false);try(var q=c.createStatement()){q.execute(sql);}
            assertThatThrownBy(()->SortChildrenService.read(c,1,false)).isInstanceOf(ResponseStatusException.class);
            if(!sql.contains("is_ro"))assertThatThrownBy(()->SortChildrenService.read(c,2,false)).isInstanceOf(ResponseStatusException.class);
            c.rollback();c.setAutoCommit(true);
        }
        assertThatThrownBy(()->SortChildrenService.read(c,0,false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->SortChildrenService.read(c,99,false)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void failedCompareAndSwapRollsBackAllReorderedRows()throws Exception{
        var state=SortChildrenService.read(c,1,false).state();
        try(var q=c.createStatement()){q.execute("CREATE TRIGGER stale AFTER UPDATE OF sequence ON children WHEN NEW.node_id=3 BEGIN UPDATE children SET sequence=77 WHERE node_id=4; END");}
        c.setAutoCommit(false);assertThatThrownBy(()->SortChildrenService.sort(c,1,SortChildrenService.Key.NAME,SortChildrenService.Order.ASC,state.revision())).isInstanceOf(ResponseStatusException.class);c.rollback();assertThat(order()).containsExactly(2L,3L,4L,7L);
    }
    @Test void serviceRequiresWritableDatabaseAndExplicitConfirmation(){
        var properties=org.mockito.Mockito.mock(com.turkerozturk.node.properties.NodePropertiesService.class);var service=new SortChildrenService(properties);
        assertThatThrownBy(()->service.state(1)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.sort(1,SortChildrenService.Key.NAME,SortChildrenService.Order.ASC,"r",true)).isInstanceOf(ResponseStatusException.class);
        org.mockito.Mockito.when(properties.writable()).thenReturn(true);
        assertThatThrownBy(()->service.sort(1,SortChildrenService.Key.NAME,SortChildrenService.Order.ASC,"r",false)).isInstanceOf(ResponseStatusException.class);
    }
}
