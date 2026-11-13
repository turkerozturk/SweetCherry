package com.turkerozturk.node.properties;

import com.turkerozturk.multipledatabases.*;
import com.turkerozturk.helpers.NodeIcon;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"myapp.openWebBrowserOnStartup=false","myapp.desktop.enabled=false","myapp.command-line-runner.enabled=false"})
class NodeTypeConversionTest {
    @Autowired NodePropertiesService service;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired CustomPropertiesHolder custom;
    @org.junit.jupiter.api.io.TempDir Path directory;
    ManagedTenantDataSource source;MultitenantDataSource routing;Map<Object,Object> original;
    @BeforeEach void setup()throws Exception{
        var file=directory.resolve("conversion.ctb");
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var q=c.createStatement();var in=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql")){
            for(String line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R"))if(line.startsWith("CREATE TABLE")||line.startsWith("INSERT INTO"))q.execute(line);
            q.execute("DELETE FROM image");q.execute("DELETE FROM grid");q.execute("DELETE FROM codebox");
            q.execute("UPDATE node SET txt='<node><rich_text weight=\"heavy\">ABCD</rich_text></node>',syntax='custom-colors',is_richtxt="+((0x3584e4L<<3)|3)+",is_ro=0,level=2,has_image=1,has_table=1,has_codebox=1,ts_lastsave=100 WHERE node_id=2");
            q.execute("INSERT INTO image VALUES(2,0,'left','',X'010203','report.pdf','',0),(2,1,'left','',X'040506','','',0)");
            q.execute("INSERT INTO grid VALUES(2,2,'left','<table><row><cell>Body</cell></row><row><cell>Head</cell></row></table>',80,90)");
            q.execute("INSERT INTO codebox VALUES(2,3,'left','print(42)','python',100,50,1,0,0)");
        }
        source=new ManagedTenantDataSource(()->{var p=new com.zaxxer.hikari.HikariDataSource();p.setJdbcUrl("jdbc:sqlite:"+file);return p;});source.activate();
        routing=(MultitenantDataSource)dataSource;original=new HashMap<>(routing.getResolvedDataSources());var next=new HashMap<Object,Object>(original);next.put("Conversion test",source);routing.setTargetDataSources(next);routing.afterPropertiesSet();
        custom.addCustomProperties("Conversion test",Map.of("custom.isWritable","true"));TenantContext.setCurrentTenant("Conversion test");
    }
    @AfterEach void close(){TenantContext.clear();custom.removeCustomProperties("Conversion test");if(original!=null){routing.setTargetDataSources(original);routing.afterPropertiesSet();}if(source!=null)source.close();}
    void sql(String sql)throws Exception{try(var c=source.getConnection();var q=c.createStatement()){q.execute(sql);}}
    void convert(NodeType type,String syntax,boolean confirmed,String revision){service.update(2,"Converted",true,null,NodeIcon.CT_HOME,false,type,syntax,"tag",true,true,confirmed,revision);}
    void original()throws Exception{
        try(var c=source.getConnection();var q=c.createStatement();var r=q.executeQuery("SELECT syntax,is_richtxt,txt FROM node WHERE node_id=2")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("custom-colors");assertThat(r.getLong(2)&1).isEqualTo(1);assertThat(r.getString(3)).startsWith("<node>");}
        try(var c=source.getConnection();var q=c.createStatement();var r=q.executeQuery("SELECT COUNT(*) FROM image WHERE node_id=2")){assertThat(r.next()).isTrue();assertThat(r.getInt(1)).isEqualTo(2);}
    }
    @Test void convertsTextAndObjectsAtomicallyAndKeepsSharedOccurrences()throws Exception{
        var preview=service.conversionPreview(2);assertThat(preview.objects()).isEqualTo(4);
        convert(NodeType.PLAIN_TEXT,null,true,preview.revision());
        try(var c=source.getConnection();var q=c.createStatement();var r=q.executeQuery("SELECT syntax,txt,is_richtxt,is_ro,level,has_image,has_table,has_codebox FROM node WHERE node_id=2")){
            assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("plain-text");assertThat(r.getString(2)).contains("A","B","C","D","report.pdf","Image","Head","Body","print(42)").doesNotContain("<rich_text");
            assertThat(r.getLong(3)&1).isZero();assertThat((r.getLong(3)>>>3)&0xffffff).isEqualTo(0x3584e4);assertThat(r.getLong(3)&2).isEqualTo(2);
            assertThat(r.getLong(5)&3).isEqualTo(3);assertThat(r.getInt(6)+r.getInt(7)+r.getInt(8)).isZero();
        }
        for(String table:List.of("image","grid","codebox"))try(var c=source.getConnection();var q=c.createStatement();var r=q.executeQuery("SELECT COUNT(*) FROM "+table+" WHERE node_id=2")){r.next();assertThat(r.getInt(1)).isZero();}
        try(var c=source.getConnection();var q=c.createStatement();var r=q.executeQuery("SELECT master_id FROM children WHERE node_id=11")){r.next();assertThat(r.getLong(1)).isEqualTo(2);}
    }
    @Test void requiresExplicitConfirmationAndRejectsStaleBinaryBytes()throws Exception{
        var revision=service.conversionPreview(2).revision();
        assertThatThrownBy(()->convert(NodeType.PLAIN_TEXT,null,false,revision)).hasMessageContaining("confirmationRequired");original();
        sql("UPDATE image SET png=X'090909' WHERE node_id=2 AND offset=0");
        assertThatThrownBy(()->convert(NodeType.CODE,"python",true,revision)).hasMessageContaining("staleConversion");original();
    }
    @Test void rollbackRestoresNodeAndObjectsIfDeletionFails()throws Exception{
        var revision=service.conversionPreview(2).revision();
        sql("CREATE TRIGGER refuse_grid_delete BEFORE DELETE ON grid BEGIN SELECT RAISE(ABORT,'test failure'); END");
        assertThatThrownBy(()->convert(NodeType.PLAIN_TEXT,null,true,revision)).isInstanceOf(RuntimeException.class);original();
    }
    @Test void supportsCodeAndRejectsLockedSharedInvalidAndChangedContent()throws Exception{
        assertThatThrownBy(()->service.conversionPreview(11)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var revision=service.conversionPreview(2).revision();
        assertThatThrownBy(()->convert(NodeType.CODE,"unknown<script>",true,revision)).hasMessageContaining("Unsupported code syntax");original();
        sql("UPDATE node SET is_ro=1 WHERE node_id=2");assertThatThrownBy(()->convert(NodeType.PLAIN_TEXT,null,true,revision)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);original();
        sql("UPDATE node SET is_ro=0,txt='<node><rich_text>Changed</rich_text></node>' WHERE node_id=2");
        assertThatThrownBy(()->convert(NodeType.PLAIN_TEXT,null,true,revision)).hasMessageContaining("staleConversion");
        // Reset offsets before checking a text-only code conversion.
        sql("DELETE FROM image");sql("DELETE FROM grid");sql("DELETE FROM codebox");
        convert(NodeType.CODE,"python",true,service.conversionPreview(2).revision());
        try(var c=source.getConnection();var q=c.createStatement();var r=q.executeQuery("SELECT syntax,txt FROM node WHERE node_id=2")){r.next();assertThat(r.getString(1)).isEqualTo("python");assertThat(r.getString(2)).isEqualTo("Changed");}
    }
}
