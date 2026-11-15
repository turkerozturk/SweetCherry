package com.turkerozturk.codebox.gallery;
import java.sql.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class CodeGalleryServiceTest {
    Connection c;
    static CodeGallerySettings settings(){return new CodeGallerySettings(14,40,2,100,12);}
    @BeforeEach void setup()throws Exception{c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,ts_creation INTEGER)");q.execute("CREATE TABLE codebox(node_id INTEGER,offset INTEGER,txt TEXT,syntax TEXT)");q.execute("INSERT INTO node VALUES(1,'A %_!',100),(2,'B <node>',200)");}}
    @AfterEach void close()throws Exception{c.close();}
    void insert(long id,int offset,String text,String syntax)throws Exception{try(var q=c.prepareStatement("INSERT INTO codebox VALUES(?,?,?,?)")){q.setLong(1,id);q.setInt(2,offset);q.setString(3,text);q.setString(4,syntax);q.executeUpdate();}}
    @Test void readOnlyPreviewsFilterSortAndPaginateWithoutLoadingWholeSources()throws Exception{
        for(int i=0;i<15;i++)insert(1,i,"line 1\nline 2\nline 3\n"+"x".repeat(300),"python3");insert(2,0,"<script>not executed</script>","../../evil");insert(99,0,"orphan","java");
        try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
        var page=CodeGalleryService.list(c,new CodeGalleryService.Options(1,"%_!","python3","id"),settings());assertThat(page.total()).isEqualTo(15);assertThat(page.items()).hasSize(12);assertThat(page.items().get(0).preview()).isEqualTo("line 1\nline 2");assertThat(page.items().get(0).language()).isEqualTo("python");assertThat(page.items().get(0).limited()).isTrue();
        assertThat(CodeGalleryService.list(c,new CodeGalleryService.Options(2,"","","id"),settings()).items()).hasSize(4);
        var newest=CodeGalleryService.list(c,new CodeGalleryService.Options(1,"","","newest"),settings()).items().get(0);assertThat(newest.nodeId()).isEqualTo(2);assertThat(newest.language()).isNull();assertThat(newest.preview()).contains("<script>");
        assertThat(CodeGalleryService.list(c,new CodeGalleryService.Options(1,"' OR 1=1 --","","id"),settings()).items()).isEmpty();
    }
    @Test void fullSourceRemainsExactWhileViewerCroppingAndOversizeRejectionAreExplicit()throws Exception{
        String source="a".repeat(99)+"😀 tail\n";insert(1,0,source,"java");
        var full=CodeGalleryService.source(c,1,0);assertThat(full.text()).isEqualTo(source);assertThat(CodeGalleryService.crop(source,100,20)).isEqualTo("a".repeat(99));
        insert(1,1,"x".repeat(CodeGalleryService.MAX_SOURCE+1),"plain-text");assertThatThrownBy(()->CodeGalleryService.source(c,1,1)).hasMessageContaining("413");
        assertThat(CodeGalleryService.list(c,new CodeGalleryService.Options(1,"","plain-text","id"),settings()).items().get(0).preview()).hasSize(40);
        assertThatThrownBy(()->CodeGalleryService.source(c,1,99)).hasMessageContaining("404");assertThatThrownBy(()->new CodeGalleryService.Options(0,"","","newest")).hasMessageContaining("400");
    }
    @Test void settingsAreBoundedAndExposedInSafeEditor(){
        assertThat(com.turkerozturk.settings.editing.ApplicationSettingsService.SETTINGS.stream().filter(s->s.key().startsWith("myapp.codebox-gallery.")).toList()).hasSize(5).allMatch(s->s.numeric()&&Long.parseLong(s.fallback())>=s.minimum()&&Long.parseLong(s.fallback())<=s.maximum());
        assertThatThrownBy(()->new CodeGallerySettings(100,40,2,100,12)).isInstanceOf(IllegalArgumentException.class);
    }
}
