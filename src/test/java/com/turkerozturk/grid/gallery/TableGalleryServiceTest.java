package com.turkerozturk.grid.gallery;
import java.sql.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class TableGalleryServiceTest {
    Connection c;
    static TableGallerySettings settings(){return new TableGallerySettings(160,6,14,160,260,5,5,200,12);}
    @BeforeEach void setup()throws Exception{
        c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){
            q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,ts_creation INTEGER)");
            q.execute("CREATE TABLE grid(node_id INTEGER,offset INTEGER,txt TEXT,col_min INTEGER,col_max INTEGER)");
            q.execute("INSERT INTO node VALUES(1,'A <node>',100),(2,'B 100%_!',200)");
        }
    }
    @AfterEach void close()throws Exception{c.close();}
    void insert(long id,int offset,String xml)throws Exception{
        try(var q=c.prepareStatement("INSERT INTO grid VALUES(?,?,?,40,400)")){q.setLong(1,id);q.setInt(2,offset);q.setString(3,xml);q.executeUpdate();}
    }
    @Test void listingIsReadOnlyStablePaginatedAndSearchIsLiteral()throws Exception{
        for(int n=0;n<15;n++)insert(1,n,"not parsed by metadata listing");insert(2,0,"<table/>");insert(99,0,"<table/>");
        try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
        var first=TableGalleryService.list(c,new TableGalleryService.Options(1,"","id"),12);
        var second=TableGalleryService.list(c,new TableGalleryService.Options(2,"","id"),12);
        assertThat(first.total()).isEqualTo(16);assertThat(first.items()).hasSize(12);assertThat(second.items()).hasSize(4);
        assertThat(second.items().get(0).offset()).isEqualTo(12);
        assertThat(TableGalleryService.list(c,new TableGalleryService.Options(1,"%_!","newest"),12).items()).extracting(TableGalleryService.Item::id).containsExactly("2:0");
        assertThat(TableGalleryService.list(c,new TableGalleryService.Options(1,"' OR 1=1 --","newest"),12).items()).isEmpty();
        assertThat(TableGalleryService.source(c,2,0)).isEqualTo("<table/>");
        assertThatThrownBy(()->new TableGalleryService.Options(1,"","node;DROP TABLE grid")).hasMessageContaining("400");
    }
    @Test void headerLastStorageIsRenderedFirstAndRaggedRowsArePadded(){
        String xml="<table col_min=\"40\"><row><cell>A &amp; B\nC</cell></row><row><cell>X</cell><cell>Y</cell></row><row><cell>Header 1</cell><cell>Header 2</cell></row></table>";
        var data=TableGalleryService.parse(xml,true,settings());
        assertThat(data.header()).containsExactly("Header 1","Header 2");assertThat(data.rows().get(0)).containsExactly("A & B\nC","");assertThat(data.totalRows()).isEqualTo(2);assertThat(data.limited()).isFalse();
        assertThat(TableGalleryService.parse("<table/>",false,settings()).columns()).isZero();
    }
    @Test void previewAndViewerHaveExplicitCellLimitsWithoutAlteringSource(){
        var b=new StringBuilder("<table>");for(int r=0;r<220;r++){b.append("<row>");for(int k=0;k<110;k++)b.append("<cell>value</cell>");b.append("</row>");}String xml=b.append("</table>").toString();
        var preview=TableGalleryService.parse(xml,false,settings());assertThat(preview.rows()).hasSize(5);assertThat(preview.columns()).isEqualTo(5);assertThat(preview.limited()).isTrue();
        var full=TableGalleryService.parse(xml,true,settings());assertThat(full.columns()).isEqualTo(100);assertThat(full.rows()).hasSize(99);assertThat(full.totalRows()).isEqualTo(219);assertThat(full.totalColumns()).isEqualTo(110);assertThat(full.limited()).isTrue();
        var longCell=TableGalleryService.parse("<table><row><cell>"+"a".repeat(239)+"😀tail</cell></row></table>",false,settings());assertThat(longCell.header().get(0)).endsWith("…").doesNotContain("\uD83D");
    }
    @Test void invalidXmlDtdNestedMarkupAndOversizedSourcesAreRejected()throws Exception{
        for(String xml:new String[]{"bad","<!DOCTYPE table [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><table><row><cell>&x;</cell></row></table>","<table><row><cell><img src='x'/></cell></row></table>"})assertThatThrownBy(()->TableGalleryService.parse(xml,true,settings())).hasMessageContaining("422");
        insert(1,0,"x".repeat(TableGalleryService.MAX_XML+1));assertThatThrownBy(()->TableGalleryService.source(c,1,0)).hasMessageContaining("413");
        assertThatThrownBy(()->TableGalleryService.source(c,1,99)).hasMessageContaining("404");
    }
    @Test void displaySettingsUseTheSameBoundsAsTheSafeSettingsEditor(){
        var settings=com.turkerozturk.settings.editing.ApplicationSettingsService.SETTINGS.stream().filter(s->s.key().startsWith("myapp.table-gallery.")).toList();assertThat(settings).hasSize(9);
        assertThat(settings).allMatch(s->s.numeric()&&Long.parseLong(s.fallback())>=s.minimum()&&Long.parseLong(s.fallback())<=s.maximum());
        assertThatThrownBy(()->new TableGallerySettings(481,6,14,160,260,5,5,200,12)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new TableGallerySettings(160,-1,14,160,260,5,5,200,12)).isInstanceOf(IllegalArgumentException.class);
    }
}
