package com.turkerozturk.calendar;

import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.richtext.experimental.RichTextRenderingService;
import jakarta.persistence.EntityManager;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalendarServiceTest {
    Connection connection;CalendarService service;NodePropertiesService properties;
    CalendarSettings settings=new CalendarSettings("MONDAY","Europe/Istanbul",10000);
    @BeforeEach void setup()throws Exception {
        connection=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var sql=connection.createStatement()){
            sql.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,tags TEXT,is_ro INTEGER,is_richtxt INTEGER,syntax TEXT,ts_creation INTEGER,ts_lastsave INTEGER)");
            sql.execute("CREATE TABLE children(node_id INTEGER PRIMARY KEY,father_id INTEGER,master_id INTEGER)");
            long date=settings.timestamp("2026-10-28T14:15");
            sql.execute("INSERT INTO node VALUES(1,'<Name>','Tag tag C++ one,two',0,3,'plain-text',"+date+",123),(2,'Locked','tag',1,0,'plain-text',"+date+",123),(3,'Outside','',0,0,'plain-text',0,0)");
            sql.execute("INSERT INTO children VALUES(1,0,0),(11,0,1),(2,0,0),(3,0,0)");
        }
        properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
        service=new CalendarService(settings,properties,mock(RichTextRenderingService.class));
        EntityManager manager=mock(EntityManager.class);Session session=mock(Session.class);when(manager.unwrap(Session.class)).thenReturn(session);
        doAnswer(call->{((org.hibernate.jdbc.Work)call.getArgument(0)).execute(connection);return null;}).when(session).doWork(any());
        doAnswer(call->((org.hibernate.jdbc.ReturningWork<?>)call.getArgument(0)).execute(connection)).when(session).doReturningWork(any());
        ReflectionTestUtils.setField(service,"entityManager",manager);
    }
    @AfterEach void close()throws Exception{connection.close();}
    @Test void monthWeekRangeAndSharedOccurrencesUseMetadataOnlyAndExactTags()throws Exception {
        var view=service.view("month",LocalDate.of(2026,10,28),List.of(),"any",Locale.ENGLISH);
        assertThat(view.days()).hasSize(42);assertThat(view.days().get(0).date()).isEqualTo(LocalDate.of(2026,9,28));assertThat(view.count()).isEqualTo(3);
        var day=view.days().stream().filter(d->d.date().equals(LocalDate.of(2026,10,28))).findFirst().orElseThrow();
        assertThat(day.entries()).extracting(CalendarService.Entry::id).containsExactly("1","11","2");assertThat(day.entries().get(1).shared()).isTrue();
        assertThat(day.entries().get(0).bold()).isTrue();assertThat(day.entries().get(0).local()).isEqualTo("2026-10-28T14:15");
        var week=service.view("week",LocalDate.of(2026,10,28),List.of("Tag","C++"),"all",Locale.forLanguageTag("tr"));assertThat(week.days()).hasSize(7);assertThat(week.hours()).hasSize(24);assertThat(week.count()).isEqualTo(2);assertThat(week.hours().get(14).cells().get(2)).hasSize(2);
        assertThat(service.view("week",LocalDate.of(2026,10,28),List.of("ta"),"any",Locale.ENGLISH).count()).isZero();
        assertThat(service.read(connection,0,Long.MAX_VALUE,List.of(),"any",1)).hasSize(1);
    }
    @Test void changingSharedDateTouchesOnlyMasterDatesAndSupportsPastAndFuture()throws Exception {
        long now=Instant.now().getEpochSecond();service.changeCreated(11,settings.timestamp("2026-10-28T14:15"),123,"2035-02-03T04:05");
        assertThat(value("ts_creation",1)).isEqualTo(settings.timestamp("2035-02-03T04:05"));assertThat(value("ts_lastsave",1)).isBetween(now,Instant.now().getEpochSecond());assertThat(value("ts_lastsave",2)).isEqualTo(123);
        service.changeCreated(1,value("ts_creation",1),value("ts_lastsave",1),"1950-01-02T03:04");assertThat(value("ts_creation",1)).isNegative();
        var result=service.view("month",LocalDate.of(1950,1,2),List.of(),"any",Locale.ENGLISH);assertThat(result.count()).isEqualTo(2);
    }
    @Test void staleClosedLockedInvalidAndMissingRequestsDoNotWrite()throws Exception {
        long created=value("ts_creation",1);
        assertThatThrownBy(()->service.changeCreated(1,created,999,"2030-01-01T00:00")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThatThrownBy(()->service.changeCreated(2,created,123,"2030-01-01T00:00")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        when(properties.writable()).thenReturn(false);assertThatThrownBy(()->service.changeCreated(1,created,123,"2030-01-01T00:00")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");when(properties.writable()).thenReturn(true);
        assertThatThrownBy(()->service.changeCreated(99,created,123,"2030-01-01T00:00")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        assertThatThrownBy(()->service.changeCreated(1,created,123,"2026-02-30T00:00")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");assertThat(value("ts_lastsave",1)).isEqualTo(123);
    }
    @Test void noChangePreservesSecondsAndModifiedDate()throws Exception {
        try(var sql=connection.createStatement()){sql.execute("UPDATE node SET ts_creation=ts_creation+35 WHERE node_id=1");}
        long before=value("ts_creation",1);service.changeCreated(1,before,123,"2026-10-28T14:15");assertThat(value("ts_creation",1)).isEqualTo(before);assertThat(value("ts_lastsave",1)).isEqualTo(123);
    }
    @Test void sundayWeeksYearBoundariesAndVisibleLimitsAreExplicit() {
        var limited=new CalendarService(new CalendarSettings("7","Europe/Istanbul",1),properties,mock(RichTextRenderingService.class));
        ReflectionTestUtils.setField(limited,"entityManager",ReflectionTestUtils.getField(service,"entityManager"));
        var month=limited.view("month",LocalDate.of(2026,10,28),List.of(),"any",Locale.ENGLISH);
        assertThat(month.days().get(0).date()).isEqualTo(LocalDate.of(2026,9,27));assertThat(month.count()).isEqualTo(1);assertThat(month.truncated()).isTrue();
        var january=limited.view("month",LocalDate.of(2027,1,1),List.of(),"any",Locale.ENGLISH);assertThat(january.days().get(0).date()).isEqualTo(LocalDate.of(2026,12,27));
        assertThat(limited.view("month",LocalDate.of(9999,12,1),List.of(),"any",Locale.ENGLISH).canNext()).isFalse();
    }
    @Test void dayUsesLocalMidnightsAndReusesMasterPreviewsWithoutBlobReads()throws Exception {
        try(var sql=connection.createStatement()){
            sql.execute("ALTER TABLE node ADD COLUMN txt TEXT");
            sql.execute("CREATE TABLE image(node_id INTEGER,anchor TEXT,filename TEXT,png BLOB)");
            sql.execute("CREATE TABLE grid(node_id INTEGER,txt TEXT)");sql.execute("CREATE TABLE codebox(node_id INTEGER,txt TEXT)");
            sql.execute("UPDATE node SET txt='<node><rich_text>First line\nSecond &amp; third</rich_text></node>',syntax='custom-colors' WHERE node_id=1");
            sql.execute("INSERT INTO image VALUES(1,'','',zeroblob(1000000)),(1,'','report.pdf',zeroblob(1000000)),(1,'anchor','',NULL)");
            sql.execute("INSERT INTO grid VALUES(1,'not fetched')");sql.execute("INSERT INTO codebox VALUES(1,'not fetched')");
            sql.execute("UPDATE node SET ts_creation="+settings.timestamp("2026-10-28T00:00")+" WHERE node_id=1");
            sql.execute("UPDATE node SET ts_creation="+(settings.timestamp("2026-10-28T23:59")+59)+" WHERE node_id=2");
            sql.execute("UPDATE node SET ts_creation="+settings.timestamp("2026-10-29T00:00")+" WHERE node_id=3");
        }
        var view=service.view("day",LocalDate.of(2026,10,28),List.of(),"any",Locale.ENGLISH);
        assertThat(view.days()).hasSize(1);assertThat(view.hours()).isEmpty();
        assertThat(view.previous()).isEqualTo(LocalDate.of(2026,10,27));assertThat(view.next()).isEqualTo(LocalDate.of(2026,10,29));
        assertThat(view.days().get(0).entries()).extracting(CalendarService.Entry::id).containsExactly("1","11","2");
        var previews=service.previews(view.days().get(0).entries());assertThat(previews).hasSize(2);
        var preview=previews.get("1");assertThat(preview.text()).isEqualTo("First line\nSecond & third");
        assertThat(preview.attachments()).isEqualTo(1);assertThat(preview.images()).isEqualTo(1);
        assertThat(preview.tables()).isEqualTo(1);assertThat(preview.codeboxes()).isEqualTo(1);
        org.springframework.test.util.ReflectionTestUtils.setField(settings,"dayEntries",1);
        assertThat(service.view("day",LocalDate.of(2026,10,28),List.of(),"any",Locale.ENGLISH).truncated()).isTrue();
    }
    @Test void dayRangeIncludesDstTransitionDaysWithoutAssumingTwentyFourHours()throws Exception {
        var berlin=new CalendarSettings("MONDAY","Europe/Berlin",10000);
        var other=new CalendarService(berlin,properties,mock(RichTextRenderingService.class));
        ReflectionTestUtils.setField(other,"entityManager",ReflectionTestUtils.getField(service,"entityManager"));
        for(var date:List.of(LocalDate.of(2026,3,29),LocalDate.of(2026,10,25))){
            long start=date.atStartOfDay(berlin.zone()).toEpochSecond(),end=date.plusDays(1).atStartOfDay(berlin.zone()).toEpochSecond();
            try(var sql=connection.createStatement()){
                sql.execute("UPDATE node SET ts_creation="+start+" WHERE node_id=1");sql.execute("UPDATE node SET ts_creation="+(end-1)+" WHERE node_id=2");sql.execute("UPDATE node SET ts_creation="+end+" WHERE node_id=3");
            }
            assertThat(other.view("day",date,List.of(),"any",Locale.ENGLISH).count()).isEqualTo(3);
        }
    }
    long value(String column,int id)throws Exception{try(var q=connection.createStatement();var r=q.executeQuery("SELECT "+column+" FROM node WHERE node_id="+id)){r.next();return r.getLong(1);}}
}
