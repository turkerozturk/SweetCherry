package com.turkerozturk.image.attachments.preview;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.zip.*;
import org.apache.commons.compress.archivers.tar.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilePreviewServiceTest {
    static FilePreviewSettings settings(){return new FilePreviewSettings(1024,200000,128,5242880,33554432,1000);}
    static FilePreviewService service(){return new FilePreviewService(settings());}
    static FilePreviewService.Preview parse(byte[] bytes,String name,String mode,String encoding){return service().parse(bytes,name,"node",bytes.length,mode,encoding,1,1,0,bytes.length);}
    @Test void textIsLiteralAndDefaultsToUtf8WithExplicitLegacyAndUtf16Choices(){
        String text="<script>alert(1)</script> Türkçe 😀\r\n\tend";
        assertThat(parse(text.getBytes(StandardCharsets.UTF_8),"sample.html","text","UTF-8").text()).isEqualTo(text);
        assertThat(parse(text.getBytes(StandardCharsets.UTF_8),"sample.html","text","UTF-8").language()).isEqualTo("xml");
        assertThat(parse("Türkçe ıİ".getBytes(java.nio.charset.Charset.forName("windows-1254")),"a.txt","text","windows-1254").text()).isEqualTo("Türkçe ıİ");
        assertThat(parse(text.getBytes(StandardCharsets.UTF_16),"a.txt","text","UTF-16").text()).isEqualTo(text);
        var bad=parse(new byte[]{(byte)0xff,0,65},"a.bin","text","UTF-8");assertThat(bad.replaced()).isTrue();assertThat(bad.binary()).isTrue();assertThat(bad.text()).contains("�","A");
    }
    @Test void textLimitsDoNotSplitCodePointsOrMisreportAnIncompleteUtf8Tail(){
        byte[] bytes="ab😀end".getBytes(StandardCharsets.UTF_8);
        var view=service().parse(bytes,"a.txt","n",bytes.length,"text","UTF-8",1,1,0,4);
        assertThat(view.text()).isEqualTo("ab");assertThat(view.clipped()).isTrue();assertThat(view.replaced()).isFalse();
        var tiny=new FilePreviewService(new FilePreviewSettings(1024,3,128,1024,1024,1));
        assertThat(tiny.parse(bytes,"a.txt","n",bytes.length,"text","UTF-8",1,1,0,bytes.length).text()).isEqualTo("ab😀");
    }
    @Test void hexPreservesExactUnsignedBytesAndOffsets(){
        byte[] data={0,31,32,60,62,65,126,127,(byte)128,(byte)255};
        var view=service().parse(data,"a.bin","n",200,"hex","UTF-8",2,2,128,128);
        assertThat(view.hex().get(0).offset()).isEqualTo("0000000000000080");assertThat(view.hex().get(0).hex()).isEqualTo("00 1F 20 3C 3E 41 7E 7F 80 FF");assertThat(view.hex().get(0).ascii()).isEqualTo(".. <>A~...");
    }
    static byte[] zip()throws Exception{
        var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){
            for(String name:List.of("folder/","../<script>.txt","folder/你好.txt")){
                var entry=new ZipEntry(name);entry.setComment("<img src=x>");zip.putNextEntry(entry);if(!name.endsWith("/"))zip.write("content".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            }
        }return out.toByteArray();
    }
    @Test void zipListsMetadataWithoutExtractingTraversalNamesOrInflatingPayloads()throws Exception{
        byte[] data=zip(),original=data.clone();var view=parse(data,"a.zip","archive","UTF-8");
        assertThat(view.entries()).hasSize(3);assertThat(view.entries().get(0).kind()).isEqualTo("directory");
        assertThat(view.entries().get(1).name()).isEqualTo("../<script>.txt");assertThat(view.entries().get(1).comment()).isEqualTo("<img src=x>");
        assertThat(view.entries().get(1).bytes()).isEqualTo(7);assertThat(view.entries().get(1).crc()).hasSize(8);assertThat(data).isEqualTo(original);
        byte[] encrypted=data.clone();for(int i=0;i+10<encrypted.length;i++)if(encrypted[i]==0x50&&encrypted[i+1]==0x4b&&encrypted[i+2]==1&&encrypted[i+3]==2)encrypted[i+8]|=1;
        assertThat(parse(encrypted,"locked.zip","archive","UTF-8").entries()).allSatisfy(entry->assertThat(entry.encrypted()).isTrue());
        var limited=new FilePreviewService(new FilePreviewSettings(1024,100,128,5242880,1024,1));
        assertThat(limited.parse(data,"a.jar","n",data.length,"archive","UTF-8",1,1,0,data.length).entries()).hasSize(1);
        assertThat(limited.parse(data,"a.jar","n",data.length,"archive","UTF-8",1,1,0,data.length).clipped()).isTrue();
        var bomb=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bomb)){zip.putNextEntry(new ZipEntry("large.bin"));zip.write(new byte[1048576]);zip.closeEntry();}
        assertThat(parse(bomb.toByteArray(),"bomb.zip","archive","UTF-8").entries().get(0).bytes()).isEqualTo(1048576);
    }
    static byte[] tar(boolean gzip)throws Exception{
        var out=new ByteArrayOutputStream();try(var tar=new TarArchiveOutputStream(out)){
            var entry=new TarArchiveEntry("../<file>.txt");entry.setSize(4096);tar.putArchiveEntry(entry);tar.write(new byte[4096]);tar.closeArchiveEntry();
            var link=new TarArchiveEntry("link",TarConstants.LF_SYMLINK);link.setLinkName("../../target");tar.putArchiveEntry(link);tar.closeArchiveEntry();
        }
        if(!gzip)return out.toByteArray();var compressed=new ByteArrayOutputStream();try(var gz=new GZIPOutputStream(compressed)){gz.write(out.toByteArray());}return compressed.toByteArray();
    }
    @Test void tarAndTgzListLinksWithoutFollowingThemAndEnforceExpansionCap()throws Exception{
        for(boolean gzip:List.of(false,true)){
            var view=parse(tar(gzip),gzip?"a.tgz":"a.tar","archive","UTF-8");assertThat(view.entries()).hasSize(2);assertThat(view.entries().get(1).target()).isEqualTo("../../target");assertThat(view.entries().get(1).kind()).isEqualTo("symlink");
        }
        var small=new FilePreviewService(new FilePreviewSettings(1024,100,128,5242880,1024,1));byte[] compressed=tar(true);
        assertThatThrownBy(()->small.parse(compressed,"a.tar.gz","n",compressed.length,"archive","UTF-8",1,1,0,compressed.length)).hasMessage("large");
        assertThatThrownBy(()->parse(new byte[]{1,2,3},"bad.zip","archive","UTF-8")).hasMessage("invalid");
        assertThatThrownBy(()->parse(new byte[1024],"bad.tgz","archive","UTF-8")).hasMessage("invalid");
    }
    @Test void unknownTypesCanAlwaysUseTextOrHexAndOptionsAreAllowlisted(){
        for(String name:List.of("sample.java","data.json","archive.tar.gz","unknown.bin"))assertThat(FilePreviewService.archive(name)).isEqualTo(name.equals("archive.tar.gz"));
        assertThat(parse(new byte[0],"unknown.bin","hex","UTF-8").hex()).isEmpty();
        assertThatThrownBy(()->new FilePreviewService.Options("text","../../UTF-8",1)).hasMessage("request");
        assertThatThrownBy(()->new FilePreviewService.Options("text","UTF-8",0)).hasMessage("request");
        assertThatThrownBy(()->new FilePreviewSettings(1024,1,129,1024,1024,1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void databaseReadsAreBoundedReadOnlyAndSelectMetadataBeforeArchiveBlobs()throws Exception{
        try(var c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            try(var q=c.createStatement()){q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT,ts_lastsave INTEGER)");q.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,anchor TEXT,png BLOB,filename TEXT,time INTEGER)");q.execute("INSERT INTO node VALUES(1,'node',123)");}
            byte[] data=new byte[2048];data[128]=65;
            try(var q=c.prepareStatement("INSERT INTO image VALUES(1,0,'',?,'unknown.bin',456)")){q.setBytes(1,data);q.executeUpdate();}
            try(var q=c.prepareStatement("INSERT INTO image VALUES(1,?,'',?,?,456)")){
                q.setInt(1,1);q.setBytes(2,"Türkçe 😀".getBytes(StandardCharsets.UTF_8));q.setString(3,"known.txt");q.executeUpdate();
                q.setInt(1,2);q.setBytes(2,zip());q.setString(3,"archive.zip");q.executeUpdate();
                q.setInt(1,3);q.setBytes(2,new byte[0]);q.setString(3,"empty.bin");q.executeUpdate();
            }
            try(var q=c.createStatement()){q.execute("PRAGMA query_only=ON");}
            var known=service().load(c,1,1,new FilePreviewService.Options("auto","UTF-8",1));assertThat(known.mode()).isEqualTo("text");assertThat(known.text()).isEqualTo("Türkçe 😀");
            assertThat(service().load(c,1,2,new FilePreviewService.Options("auto","UTF-8",1)).mode()).isEqualTo("archive");
            assertThat(service().load(c,1,3,new FilePreviewService.Options("auto","UTF-8",1)).hex()).isEmpty();
            var view=service().load(c,1,0,new FilePreviewService.Options("auto","UTF-8",2));
            assertThat(view.mode()).isEqualTo("hex");assertThat(view.start()).isEqualTo(128);assertThat(view.shown()).isEqualTo(128);assertThat(view.hex().get(0).ascii()).startsWith("A");assertThat(view.pages()).isEqualTo(16);
            var text=service().load(c,1,0,new FilePreviewService.Options("text","UTF-8",1));assertThat(text.shown()).isEqualTo(1024);assertThat(text.clipped()).isTrue();
            assertThatThrownBy(()->service().load(c,1,0,new FilePreviewService.Options("hex","UTF-8",17))).hasMessage("request");
            assertThatThrownBy(()->service().load(c,1,0,new FilePreviewService.Options("archive","UTF-8",1))).hasMessage("unsupported");
            assertThatThrownBy(()->service().load(c,1,9,new FilePreviewService.Options("auto","UTF-8",1))).hasMessage("missing");
            try(var q=c.createStatement();var rs=q.executeQuery("SELECT png,time,ts_lastsave FROM image JOIN node USING(node_id) WHERE offset=0")){assertThat(rs.next()).isTrue();assertThat(rs.getBytes(1)).isEqualTo(data);assertThat(rs.getInt(2)).isEqualTo(456);assertThat(rs.getInt(3)).isEqualTo(123);}
        }
    }
    @Test void oversizedArchivesAreRejectedBeforeAnyBlobQuery()throws Exception{
        var c=mock(Connection.class);var metadata=mock(DatabaseMetaData.class);var q=mock(PreparedStatement.class);var rs=mock(ResultSet.class);
        when(c.getMetaData()).thenReturn(metadata);when(metadata.getDatabaseProductName()).thenReturn("SQLite");when(metadata.getIdentifierQuoteString()).thenReturn("\"");
        when(c.prepareStatement(anyString())).thenReturn(q);when(q.executeQuery()).thenReturn(rs);when(rs.next()).thenReturn(true);when(rs.getString(1)).thenReturn("large.zip");when(rs.getString(2)).thenReturn("node");when(rs.getLong(3)).thenReturn(2048L);
        var small=new FilePreviewService(new FilePreviewSettings(1024,100,128,1024,1024,1));
        assertThatThrownBy(()->small.load(c,1,0,new FilePreviewService.Options("archive","UTF-8",1))).hasMessage("large");
        verify(c,times(1)).prepareStatement(anyString());verify(rs,never()).getBinaryStream(anyInt());
    }

}
