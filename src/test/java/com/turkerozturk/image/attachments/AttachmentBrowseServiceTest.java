package com.turkerozturk.image.attachments;
import java.sql.*;
import java.io.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class AttachmentBrowseServiceTest {
    Connection c;
    @BeforeEach void setup()throws Exception{c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT)");q.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,anchor TEXT,png BLOB,filename TEXT,time INTEGER)");q.execute("INSERT INTO node VALUES(1,'<node>')");}}
    @AfterEach void close()throws Exception{c.close();}
    void insert(int offset,String filename,String anchor,int size)throws Exception{try(var q=c.prepareStatement("INSERT INTO image VALUES(1,?,?,?, ?,100)")){q.setInt(1,offset);q.setString(2,anchor);q.setBytes(3,new byte[size]);q.setString(4,filename);q.executeUpdate();}}
    @Test void namedFilesIncludeImageAttachmentsButExcludePicturesAnchorsAndLatex()throws Exception{
        insert(0,"alpha.pdf","",9);insert(1,"Zulu.PNG","",100);insert(2,"beta.txt","",20);insert(3,"__ct_special.tex","",3);insert(4,"anchor.txt","anchor",1);insert(5,"","",20);insert(6,null,"",20);
        try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
        assertThat(AttachmentBrowseService.list(c,new AttachmentBrowseService.Options(1,"","name-asc")).items()).extracting(AttachmentBrowseService.Item::filename).containsExactly("alpha.pdf","beta.txt","Zulu.PNG");
        assertThat(AttachmentBrowseService.list(c,new AttachmentBrowseService.Options(1,"","size-desc")).items()).extracting(AttachmentBrowseService.Item::bytes).containsExactly(100L,20L,9L);
        assertThat(AttachmentBrowseService.list(c,new AttachmentBrowseService.Options(1,"","size-asc")).items()).extracting(AttachmentBrowseService.Item::bytes).containsExactly(9L,20L,100L);
        assertThat(AttachmentBrowseService.list(c,new AttachmentBrowseService.Options(1,"","name-desc")).items().get(0).filename()).isEqualTo("Zulu.PNG");
    }
    @Test void literalNameFilteringPaginationAndStableTiesWork()throws Exception{
        for(int i=0;i<55;i++)insert(i,"100%_!.txt","",i);
        assertThat(AttachmentBrowseService.list(c,new AttachmentBrowseService.Options(2,"%_!","newest")).items()).hasSize(5).extracting(AttachmentBrowseService.Item::offset).containsExactly(50,51,52,53,54);
        assertThat(AttachmentBrowseService.list(c,new AttachmentBrowseService.Options(1,"' OR 1=1 --","newest")).items()).isEmpty();assertThatThrownBy(()->new AttachmentBrowseService.Options(1,"","invalid")).hasMessageContaining("400");
    }
    @Test void singleFileStreamsExactlyAndCannotReadNonAttachments()throws Exception{
        insert(0,"../folder/evil\r\n\"name.txt","",23);insert(1,"","",8);insert(2,"__ct_special.tex","",8);var out=new ByteArrayOutputStream();var meta=new java.util.ArrayList<AttachmentBrowseService.Download>();
        try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
        AttachmentBrowseService.stream(c,1,0,meta::add,out);assertThat(out.toByteArray()).isEqualTo(new byte[23]);assertThat(meta.get(0).filename()).isEqualTo("evil\"name.txt");assertThat(meta.get(0).bytes()).isEqualTo(23);
        assertThatThrownBy(()->AttachmentBrowseService.stream(c,1,1,meta::add,out)).hasMessageContaining("404");assertThatThrownBy(()->AttachmentBrowseService.stream(c,1,2,meta::add,out)).hasMessageContaining("404");assertThatThrownBy(()->AttachmentBrowseService.stream(c,1,-1,meta::add,out)).hasMessageContaining("400");
    }
}
