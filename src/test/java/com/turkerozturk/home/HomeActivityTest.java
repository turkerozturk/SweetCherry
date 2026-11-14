package com.turkerozturk.home;

import com.turkerozturk.calendar.CalendarSettings;
import com.turkerozturk.multipledatabases.*;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomeActivityTest {
    @TempDir Path directory;
    private final HomeSettings settings = new HomeSettings();
    private final CalendarSettings calendar = new CalendarSettings("MONDAY", "Europe/Istanbul", 10000);
    private final TenantService tenants = mock(TenantService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC);
    private final HomeActivityService service = new HomeActivityService(settings, calendar, tenants);
    private final long today = LocalDate.of(2026,11,2).atStartOfDay(calendar.zone()).toEpochSecond();
    @AfterEach void clear() { TenantContext.clear(); }
    private SQLiteDataSource database(String name) throws Exception {
        var source = new SQLiteDataSource(); source.setUrl("jdbc:sqlite:" + directory.resolve(name + ".ctb"));
        try (var c=source.getConnection(); var s=c.createStatement()) {
            s.execute("CREATE TABLE children(node_id INTEGER, father_id INTEGER, master_id INTEGER)");
            // Deliberately no txt/BLOB columns: home must not load note contents.
            s.execute("CREATE TABLE node(node_id INTEGER,name TEXT,syntax TEXT,is_ro INTEGER,is_richtxt INTEGER,ts_creation INTEGER,ts_lastsave INTEGER,tags TEXT)");
        }
        return source;
    }
    private void node(DataSource source,long id,long parent,long created,long modified) throws Exception {
        try(var c=source.getConnection();var s=c.createStatement()) {
            s.execute("INSERT INTO children VALUES("+id+","+parent+",0)");
            s.execute("INSERT INTO node(node_id,name,syntax,is_ro,is_richtxt,ts_creation,ts_lastsave) VALUES("+id+",'Node "+id+"','plain-text',0,0,"+created+","+modified+")");
        }
    }
    @Test void todayIsUncappedAndOlderNodesFillOnlyToTheMinimum() throws Exception {
        var db=database("activity");
        for(int i=1;i<=8;i++)node(db,i,0,today-i,today+(i<=6?i:-i));
        node(db,9,0,today+86400,today+86400); // Tomorrow is not today's activity.
        var activity=service.read(db,clock);
        assertThat(activity.modified()).extracting(HomeActivityService.Entry::id).containsExactly(6L,5L,4L,3L,2L,1L);
        assertThat(activity.created()).extracting(HomeActivityService.Entry::id).containsExactly(1L,2L,3L,4L,5L);
        assertThat(activity.created()).allMatch(e->!e.today());
        settings.setCreatedMinimum(2);
        assertThat(service.read(db,clock).created()).hasSize(2);
    }
    @Test void sharedOccurrenceUsesItsOwnPathAndMastersContentMetadata() throws Exception {
        var db=database("shared");node(db,1,0,today,today);node(db,2,0,today,today+1);
        try(var c=db.getConnection();var s=c.createStatement()) {
            s.execute("INSERT INTO children VALUES(11,2,1)");
            s.execute("UPDATE node SET is_richtxt=2 WHERE node_id=1");
        }
        var shared=service.read(db,clock).modified().stream().filter(e->e.id()==11).findFirst().orElseThrow();
        assertThat(shared.name()).isEqualTo("Node 1");
        assertThat(shared.breadcrumbs()).extracting(HomeActivityService.Crumb::id).containsExactly(2L,11L);
        assertThat(shared.date()).isEqualTo("2026-11-02 00:00:00");
        assertThat(service.latest(db)).hasValue(2L);
    }
    @Test void defaultsDisabledListsAndEmptyDatabase() throws Exception {
        assertThat(settings.isOpenLatestModified()).isFalse();
        assertThat(settings.getCreatedMinimum()).isEqualTo(5);
        assertThatThrownBy(()->settings.setModifiedMinimum(-1)).isInstanceOf(IllegalArgumentException.class);
        var empty=database("empty"); assertThat(service.latest(empty)).isEmpty();
        assertThat(service.read(empty,clock).modified()).isEmpty();
        settings.setCreatedMinimum(0);settings.setModifiedMinimum(0);
        service.populate(new ExtendedModelMap());verifyNoInteractions(tenants);
        var connection=mock(Connection.class);var source=mock(DataSource.class);when(source.getConnection()).thenReturn(connection);
        assertThat(service.read(source,clock).created()).isEmpty();verify(connection,never()).prepareStatement(anyString());
    }
    @Test void calendarTimeZoneAndMidnightBoundaryAreUsed() throws Exception {
        var db=database("midnight");node(db,1,0,today-1,today-1);node(db,2,0,today,today);
        settings.setCreatedMinimum(1);settings.setModifiedMinimum(1);
        var entries=service.read(db,clock).created();assertThat(entries).hasSize(1);
        assertThat(entries.get(0).id()).isEqualTo(2);assertThat(entries.get(0).today()).isTrue();
    }
    @Test void selectionReadsNewSourceAndPreservesViewTokenAndDefaultRoot() throws Exception {
        var old=database("old");node(old,1,0,today,today+20);
        var fresh=database("fresh");node(fresh,7,0,today,today+5);
        when(tenants.getAllTenants()).thenReturn(Map.of("Old",old,"New",fresh));
        var controller=new DatabaseSwitchController();
        ReflectionTestUtils.setField(controller,"tenantService",tenants);
        ReflectionTestUtils.setField(controller,"homeSettings",settings);
        ReflectionTestUtils.setField(controller,"homeActivity",service);
        var request=new MockHttpServletRequest();TenantContext.setCurrentTenant("Old");
        assertThat(controller.setTenant("New",request)).isEqualTo("redirect:/");
        settings.setOpenLatestModified(true);TenantContext.setCurrentTenant("Old");
        String redirect=controller.setTenant("New",request);
        assertThat(redirect).isEqualTo("redirect:/nodes/7?_tenantView="+request.getSession().getAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN));
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("New");
    }
    @Test void failedHomeQueryDoesNotPreventDatabaseHomeFromOpening() {
        TenantContext.setCurrentTenant("Closed");when(tenants.getAllTenants()).thenReturn(Map.of());
        var model=new ExtendedModelMap();service.populate(model);
        assertThat(model.get("homeActivityUnavailable")).isEqualTo(true);
        assertThat(model.get("homeModified")).isEqualTo(List.of());
    }
    @Test void auditHelpersCannotReplaceTheLatestNoteOrPolluteHomeActivity()throws Exception{
        var db=database("audit");node(db,1,0,today,today);node(db,2,0,today,today+100);
        try(var c=db.getConnection();var q=c.prepareStatement("UPDATE node SET tags=? WHERE node_id=2")){q.setString(1,com.turkerozturk.node.audit.AuditSql.TAG);q.executeUpdate();}
        assertThat(service.latest(db)).hasValue(1);assertThat(service.read(db,clock).created()).extracting(HomeActivityService.Entry::id).containsExactly(1L);assertThat(service.read(db,clock).modified()).extracting(HomeActivityService.Entry::id).containsExactly(1L);
    }

}
