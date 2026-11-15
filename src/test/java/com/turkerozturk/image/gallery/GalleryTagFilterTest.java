package com.turkerozturk.image.gallery;
import java.sql.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class GalleryTagFilterTest {
    @Test void exactCaseSensitiveLiteralTagsCombineWithSearchAndFilterBeforePagination()throws Exception{
        try(var c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            try(var q=c.createStatement()){
                q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,ts_creation INTEGER,tags TEXT)");
                q.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,anchor TEXT,png BLOB,filename TEXT,link TEXT,time INTEGER)");
                q.execute("INSERT INTO node VALUES(1,'Selected pictures',100,'  work   %_! タグ Work  '),(2,'Other pictures',200,'workshop'),(3,'Different case',300,'Work'),(4,'No tags',400,NULL),(5,'No images',500,'absent')");
            }
            try(var q=c.prepareStatement("INSERT INTO image VALUES(1,?,'',X'01','','',0)")){for(int i=0;i<40;i++){q.setInt(1,i);q.executeUpdate();}}
            try(var q=c.createStatement()){q.execute("INSERT INTO image VALUES(2,0,'',X'01','','',0),(3,0,'',X'01','','',0),(4,0,'',X'01','','',0)");q.execute("PRAGMA query_only=ON");}
            var first=GalleryService.list(c,new GalleryService.Options(1,"Selected","id","work"));
            var second=GalleryService.list(c,new GalleryService.Options(2,"Selected","id","work"));
            assertThat(first.total()).isEqualTo(40);assertThat(first.items()).hasSize(36);assertThat(second.items()).hasSize(4);assertThat(second.items().get(0).offset()).isEqualTo(36);
            assertThat(first.tags()).contains("work","Work","workshop","%_!","タグ").doesNotContain("absent");
            assertThat(GalleryService.list(c,new GalleryService.Options(1,"","id","Work")).total()).isEqualTo(41);
            assertThat(GalleryService.list(c,new GalleryService.Options(1,"","id","%_!")).total()).isEqualTo(40);
            assertThat(GalleryService.list(c,new GalleryService.Options(1,"","id","タグ")).total()).isEqualTo(40);
            assertThat(GalleryService.list(c,new GalleryService.Options(1,"","id","wor")).total()).isZero();
            assertThat(GalleryService.list(c,new GalleryService.Options(1,"","id","'OR1=1--")).total()).isZero();
            assertThat(GalleryService.list(c,new GalleryService.Options(1,"","id","")).total()).isEqualTo(43);
            assertThatThrownBy(()->new GalleryService.Options(1,"","id","two tags")).hasMessageContaining("400");
        }
    }
}
