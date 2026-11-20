package com.turkerozturk.image.attachments.drawio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DrawioServiceTest {
    static String model(String value){return "<mxGraphModel><root><mxCell id='0'/><mxCell id='1' parent='0'/><mxCell id='2' value='"+value+"' style='rounded=1;fillColor=#ffcc00;html=1;' vertex='1' parent='1'><mxGeometry x='20' y='30' width='120' height='60' as='geometry'/></mxCell></root></mxGraphModel>";}
    static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
    static DrawioService.Preview parse(String value){return DrawioDocument.parse(bytes(value),"diagram.drawio","Node");}
    static String compressed(String value)throws Exception{
        String escaped=java.net.URLEncoder.encode(value,StandardCharsets.UTF_8);var out=new ByteArrayOutputStream();var deflater=new Deflater(6,true);
        try(var stream=new DeflaterOutputStream(out,deflater)){stream.write(bytes(escaped));}finally{deflater.end();}return Base64.getEncoder().encodeToString(out.toByteArray());
    }
    @Test void dozensOfMixedCompressedAndNormalPagesRemainSelectableAndNamed()throws Exception{
        var file=new StringBuilder("<mxfile>");for(int n=1;n<=48;n++)file.append("<diagram name='Page ").append(n).append("'>").append(n%2==0?compressed(model("Türkçe + &amp; text")):model("Türkçe + &amp; text")).append("</diagram>");file.append("</mxfile>");
        var preview=parse(file.toString());assertThat(preview.pages()).hasSize(48);assertThat(preview.pages().get(47).name()).isEqualTo("Page 48");
        assertThat(preview.pages().get(0).xml()).contains("Türkçe + &amp; text","rounded=1","fillColor=#ffcc00","html=0");
    }
    @Test void supportsStandaloneAndEmptyPagesAndPreservesPlainTextAndRelativeGeometry(){
        assertThat(parse(model("hello")).pages()).hasSize(1);
        var plain=model("List&lt;T&gt;").replace("html=1","html=0").replace("x='20'","relative='1' x='0.5'");
        assertThat(parse(plain).pages().get(0).xml()).contains("List&lt;T&gt;","relative=\"1\"","x=\"0.5\"");
        assertThat(parse("<mxfile><diagram name='Blank'><mxGraphModel><root><mxCell id='0'/><mxCell id='1' parent='0'/></root></mxGraphModel></diagram></mxfile>").pages()).hasSize(1);
    }
    @Test void richLabelsAndImagesBecomeInertTextAndResourceStylesAreRemoved(){
        String value="&lt;b&gt;Bold&lt;/b&gt;&lt;br&gt;next&lt;script&gt;evil()&lt;/script&gt;&lt;img src=&quot;https://example.test/a.png?secret=1&quot; onerror=&quot;evil()&quot;&gt;";
        String xml=parse(model(value).replace("rounded=1;","rounded=1;image=https://example.test/other.jpg;shape=image;fillColor=url(https://evil);fontSource=https://evil;link=javascript:evil();")).pages().get(0).xml();
        assertThat(xml).contains("Bold","next","[Image: a.png]","[Image: other.jpg]","shape=rectangle;").doesNotContain("https://","javascript:","onerror","evil()","&lt;b&gt;","url(");
    }
    @Test void userObjectLabelsAndGroupsAreKeptWithoutCustomProperties(){
        String xml="<mxGraphModel><root><mxCell id='0'/><mxCell id='1' parent='0'/><object id='g' label='Group' link='https://evil' custom='secret'><mxCell vertex='1' parent='1'><mxGeometry width='200' height='200' as='geometry'/></mxCell></object><mxCell id='child' parent='g' value='Child' vertex='1'><mxGeometry x='10' y='10' width='50' height='50' as='geometry'/></mxCell></root></mxGraphModel>";
        String clean=parse(xml).pages().get(0).xml();assertThat(clean).contains("id=\"g\"","parent=\"g\"","value=\"Group\"").doesNotContain("secret","https://");
    }
    @Test void malformedExternalEntitiesCyclesAndNonFiniteCoordinatesAreRejected()throws Exception{
        for(String xml:List.of("<svg/>","<mxfile/>","<!DOCTYPE mxfile [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><mxfile>&x;</mxfile>",model("bad").replace("parent='1'","parent='2'"),model("bad").replace("x='20'","x='NaN'"),"<mxfile><diagram>not base64</diagram></mxfile>"))assertThatThrownBy(()->parse(xml)).isInstanceOf(DrawioService.Problem.class);
        var deep="<x>".repeat(70)+"</x>".repeat(70);assertThatThrownBy(()->parse(deep)).isInstanceOf(DrawioService.Problem.class);
    }
    @Test void expansionPageCountAndCellLimitsFailExplicitly()throws Exception{
        String bomb=compressed("x".repeat(DrawioDocument.PAGE_BYTES+1));assertThatThrownBy(()->parse("<mxfile><diagram>"+bomb+"</diagram></mxfile>")).hasMessage("large");
        String pages="<mxfile>"+("<diagram>"+model("x")+"</diagram>").repeat(129)+"</mxfile>";assertThatThrownBy(()->parse(pages)).hasMessage("large");
        String many="<mxGraphModel><root>";var buffer=new StringBuilder(many);for(int n=0;n<=4000;n++)buffer.append("<mxCell id='").append(n).append("'/>");buffer.append("</root></mxGraphModel>");assertThatThrownBy(()->parse(buffer.toString())).hasMessage("large");
    }
    @Test void manualSampleContainsVectorShapesACompressedPageAndAnExternalImagePlaceholder()throws Exception{
        byte[] data=java.nio.file.Files.readAllBytes(java.nio.file.Path.of("documentation/examples/drawio-viewer-demo.drawio"));
        var document=DrawioDocument.parse(data,"demo.drawio","Demo");assertThat(document.pages()).hasSize(3);
        assertThat(document.pages().get(0).xml()).contains("source=\"a\"","target=\"b\"","endArrow=classic");
        assertThat(document.pages().get(1).xml()).contains("Türkçe + text");assertThat(document.pages().get(2).xml()).contains("[Image: screenshot.png]").doesNotContain("https://");
    }
    @Test void readOnlyTenantLoadDoesNotModifyAttachmentsAndChecksMetadataBeforeBlob()throws Exception{
        try(var c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            try(var q=c.createStatement()){q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,name TEXT)");q.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,filename TEXT,anchor TEXT,png BLOB)");q.execute("INSERT INTO node VALUES(1,'Read-only note')");}
            byte[] original=bytes(model("hello"));try(var q=c.prepareStatement("INSERT INTO image VALUES(1,7,'a.DRAWIO','',?)")){q.setBytes(1,original);q.executeUpdate();}
            assertThat(DrawioService.load(c,1,7).filename()).isEqualTo("a.DRAWIO");
            try(var q=c.createStatement();var rs=q.executeQuery("SELECT png FROM image")){rs.next();assertThat(rs.getBytes(1)).isEqualTo(original);}
            try(var q=c.createStatement()){q.execute("UPDATE image SET png=zeroblob("+(DrawioService.MAX_BYTES+1)+")");}assertThatThrownBy(()->DrawioService.load(c,1,7)).hasMessage("large");
            try(var q=c.createStatement()){q.execute("UPDATE image SET filename='not.txt'");}assertThatThrownBy(()->DrawioService.load(c,1,7)).hasMessage("unsupported");
            assertThatThrownBy(()->DrawioService.load(c,1,99)).hasMessage("missing");
        }
    }
}
