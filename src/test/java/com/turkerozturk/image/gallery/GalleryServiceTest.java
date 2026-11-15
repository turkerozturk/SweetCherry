package com.turkerozturk.image.gallery;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;

class GalleryServiceTest {
    private Connection c;
    @BeforeEach void setup() throws Exception {
        c = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (var s = c.createStatement()) {
            s.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,ts_creation INTEGER,tags TEXT)");
            s.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,anchor TEXT,png BLOB,filename TEXT,link TEXT,time INTEGER)");
            s.execute("INSERT INTO node VALUES(1,'A <node>',100,''),(2,'B 100%_!',200,'')");
        }
    }
    @AfterEach void close() throws Exception { c.close(); }
    void image(long id, int offset, String filename, String anchor) throws Exception {
        try (var q=c.prepareStatement("INSERT INTO image VALUES(?,?,?,X'010203',?,'https://example.test/original',500)")) {
            q.setLong(1,id);q.setInt(2,offset);q.setString(3,anchor);q.setString(4,filename);q.executeUpdate();
        }
    }
    @Test void readOnlyListingIncludesRasterAttachmentsButNotOtherObjectsOrDuplicateSharedOccurrences() throws Exception {
        image(1,0,"","");image(1,1,"photo.JPG","");image(1,2,"notes.pdf","");
        image(1,3,"__ct_special.tex","");image(1,4,"","anchor");image(1,5,"unsafe.svg","");
        image(2,0,null,"");image(99,0,"","");
        try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
        var page=GalleryService.list(c,new GalleryService.Options(1,"","newest"));
        assertThat(page.total()).isEqualTo(3);assertThat(page.items()).extracting(GalleryService.Item::id).containsExactly("2:0","1:0","1:1");
        assertThat(page.items().get(1).link()).isEqualTo("https://example.test/original");
        assertThat(GalleryService.payload(c,1,1).bytes()).containsExactly(1,2,3);
        assertThatThrownBy(()->GalleryService.payload(c,1,2)).hasMessageContaining("404");
    }
    @Test void paginatesStableMetadataWithoutLoadingBlobAndEscapesLiteralSearchWildcards() throws Exception {
        for(int n=0;n<40;n++)image(1,n,"","");image(2,0,"100%_!.png","");
        var first=GalleryService.list(c,new GalleryService.Options(1,"","id"));
        var second=GalleryService.list(c,new GalleryService.Options(2,"","id"));
        assertThat(first.total()).isEqualTo(41);assertThat(first.items()).hasSize(36);assertThat(second.items()).hasSize(5);
        assertThat(second.items().get(0).offset()).isEqualTo(36);
        assertThat(GalleryService.list(c,new GalleryService.Options(1,"%_!","newest")).items()).hasSize(1);
        assertThat(GalleryService.list(c,new GalleryService.Options(1,"' OR 1=1 --","newest")).items()).isEmpty();
        assertThatThrownBy(()->new GalleryService.Options(1,"","name;DROP TABLE image")).hasMessageContaining("400");
        assertThatThrownBy(()->new GalleryService.Options(0,"","newest")).hasMessageContaining("400");
    }
    @Test void oversizedOrMissingPayloadsAreRejectedWithoutReadingThem() throws Exception {
        try(var q=c.createStatement()){q.execute("INSERT INTO image VALUES(1,0,'',zeroblob(67108865),'','',0)");}
        assertThat(GalleryService.list(c,new GalleryService.Options(1,"","newest")).items().get(0).bytes()).isEqualTo(67108865);
        assertThatThrownBy(()->GalleryService.payload(c,1,0)).hasMessageContaining("413");
        assertThatThrownBy(()->GalleryService.payload(c,1,99)).hasMessageContaining("404");
        assertThatThrownBy(()->GalleryService.payload(c,1,-1)).hasMessageContaining("400");
    }
}
