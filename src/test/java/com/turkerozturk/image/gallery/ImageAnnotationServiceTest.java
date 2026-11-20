package com.turkerozturk.image.gallery;

import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class ImageAnnotationServiceTest {
 private Connection c;
 @BeforeEach void setup()throws Exception{
  c=DriverManager.getConnection("jdbc:sqlite::memory:");try(var q=c.createStatement()){
   q.execute("CREATE TABLE node(node_id INTEGER PRIMARY KEY,txt TEXT,tags TEXT,syntax TEXT,is_ro INTEGER,ts_lastsave INTEGER)");
   q.execute("CREATE TABLE children(node_id INTEGER PRIMARY KEY,master_id INTEGER)");
   q.execute("CREATE TABLE image(node_id INTEGER,offset INTEGER,png BLOB,filename TEXT,anchor TEXT,justification TEXT,link TEXT,time INTEGER,PRIMARY KEY(node_id,offset))");
   q.execute("INSERT INTO node VALUES(1,'<node/>','','custom-colors',0,10)");q.execute("INSERT INTO children VALUES(1,0)");
  }
  try(var q=c.prepareStatement("INSERT INTO image VALUES(1,7,?,'screenshot.png','','center','node 3',123)")){q.setBytes(1,picture(0xff112233,20,10));q.executeUpdate();}
  try(var q=c.createStatement()){q.execute("INSERT INTO image SELECT node_id,8,png,'other.png',anchor,justification,link,time FROM image WHERE offset=7");}
 }
 @AfterEach void close()throws Exception{c.close();}
 @Test void aReadOnlyConnectionCannotReadOrReplaceThroughTheEditor(){
  var properties=org.mockito.Mockito.mock(com.turkerozturk.node.properties.NodePropertiesService.class);var service=new ImageAnnotationService(properties);
  assertThatThrownBy(()->service.open(1,7)).isInstanceOf(ResponseStatusException.class).satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  assertThatThrownBy(()->service.save(1,7,"r",new byte[0],true)).isInstanceOf(ResponseStatusException.class);
 }
 static byte[] picture(int color,int w,int h)throws Exception{var image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,color);var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);return out.toByteArray();}
 byte[] stored()throws Exception{try(var q=c.createStatement();var r=q.executeQuery("SELECT png FROM image WHERE node_id=1 AND offset=7")){r.next();return r.getBytes(1);}}
 @Test void replacesOnlyTheSelectedPayloadAndTouchesTheParentWithoutChangingMetadata()throws Exception{
  var snapshot=ImageAnnotationService.open(c,1,7);assertThat(snapshot.width()).isEqualTo(20);assertThat(snapshot.height()).isEqualTo(10);
  byte[] changed=ImageAnnotationService.normalize(picture(0xffe01b24,20,10));c.setAutoCommit(false);ImageAnnotationService.save(c,1,7,snapshot.revision(),changed);c.commit();
  assertThat(stored()).isEqualTo(changed);
  assertThat(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(stored())).getRGB(0,0)).isEqualTo(0xffe01b24);
  try(var q=c.createStatement();var r=q.executeQuery("SELECT png FROM image WHERE offset=8")){r.next();assertThat(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(r.getBytes(1))).getRGB(0,0)).isEqualTo(0xff112233);}
  try(var q=c.createStatement();var r=q.executeQuery("SELECT i.filename,i.anchor,i.justification,i.link,i.time,n.txt,n.ts_lastsave FROM image i JOIN node n ON n.node_id=i.node_id WHERE i.offset=7")){r.next();assertThat(r.getString(1)).isEqualTo("screenshot.png");assertThat(r.getString(2)).isEmpty();assertThat(r.getString(3)).isEqualTo("center");assertThat(r.getString(4)).isEqualTo("node 3");assertThat(r.getLong(5)).isEqualTo(123);assertThat(r.getString(6)).isEqualTo("<node/>");assertThat(r.getLong(7)).isGreaterThan(10);}
  assertThatThrownBy(()->ImageAnnotationService.save(c,1,7,snapshot.revision(),changed)).isInstanceOf(ResponseStatusException.class).satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));c.rollback();
 }
 @Test void metadataOrSourceChangesRejectOldEditorsAndOtherRowsAreUntouched()throws Exception{
  var snapshot=ImageAnnotationService.open(c,1,7);byte[] old=stored();
  try(var q=c.createStatement()){q.execute("UPDATE image SET link='https://example.org' WHERE node_id=1");}
  assertThatThrownBy(()->ImageAnnotationService.save(c,1,7,snapshot.revision(),old)).isInstanceOf(ResponseStatusException.class);
  assertThat(stored()).isEqualTo(old);
  var current=ImageAnnotationService.open(c,1,7);assertThatThrownBy(()->ImageAnnotationService.save(c,1,7,current.revision(),picture(0,21,10))).isInstanceOf(ResponseStatusException.class);
  try(var q=c.createStatement()){q.execute("UPDATE node SET is_ro=1");}
  assertThatThrownBy(()->ImageAnnotationService.open(c,1,7)).isInstanceOf(ResponseStatusException.class).satisfies(e->assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
 }
 @Test void helperNodesAnchorsAttachmentsAndSharedOwnersAreNotEditable()throws Exception{
  for(String change:List.of("UPDATE node SET tags='__sweetcherry_reminders_v1__'","UPDATE image SET anchor='anchor'","UPDATE image SET filename='file.docx'","UPDATE children SET master_id=42")){
   c.setAutoCommit(false);try(var q=c.createStatement()){q.execute(change);}assertThatThrownBy(()->ImageAnnotationService.open(c,1,7)).isInstanceOf(ResponseStatusException.class);c.rollback();
  }
 }
 @Test void aFailedBinaryCompareRollsBackTheParentTimestampAndImageChanges()throws Exception{
  var snapshot=ImageAnnotationService.open(c,1,7);byte[] original=stored();
  try(var q=c.createStatement()){q.execute("CREATE TRIGGER conflict AFTER UPDATE OF ts_lastsave ON node BEGIN UPDATE image SET png=X'01' WHERE node_id=NEW.node_id; END");}
  c.setAutoCommit(false);assertThatThrownBy(()->ImageAnnotationService.save(c,1,7,snapshot.revision(),original)).isInstanceOf(ResponseStatusException.class);c.rollback();assertThat(stored()).isEqualTo(original);
  try(var q=c.createStatement();var r=q.executeQuery("SELECT ts_lastsave FROM node")){r.next();assertThat(r.getLong(1)).isEqualTo(10);}
 }
 @Test void jpegAndBmpAttachmentsAreRenamedToPngWhileOtherMetadataIsRetained()throws Exception{
  for(String format:List.of("jpeg","bmp")){
   byte[] original=AnnotationRasterTest.picture(format);
   try(var q=c.prepareStatement("UPDATE image SET png=?,filename=? WHERE offset=7")){q.setBytes(1,original);q.setString(2,"photo."+format);q.executeUpdate();}
   var snapshot=ImageAnnotationService.open(c,1,7);assertThat(snapshot.png()).startsWith("data:image/png;base64,");
   byte[] replacement=java.util.Base64.getDecoder().decode(snapshot.png().substring(snapshot.png().indexOf(',')+1));
   c.setAutoCommit(false);ImageAnnotationService.save(c,1,7,snapshot.revision(),replacement);c.commit();c.setAutoCommit(true);
   try(var q=c.createStatement();var rs=q.executeQuery("SELECT filename,link,justification,time FROM image WHERE offset=7")){rs.next();assertThat(rs.getString(1)).isEqualTo("photo.png");assertThat(rs.getString(2)).isEqualTo("node 3");assertThat(rs.getString(3)).isEqualTo("center");assertThat(rs.getLong(4)).isEqualTo(123);}
   assertThat(ImageAnnotationService.open(c,1,7).width()).isEqualTo(20);
  }
 }
 @Test void orientedJpegReplacementUsesTheDisplayedDimensions()throws Exception{
  byte[] source=AnnotationRasterTest.oriented(AnnotationRasterTest.picture("jpeg"),6);
  try(var q=c.prepareStatement("UPDATE image SET png=?,filename='rotated.jpg' WHERE offset=7")){q.setBytes(1,source);q.executeUpdate();}
  var snapshot=ImageAnnotationService.open(c,1,7);assertThat(snapshot.width()).isEqualTo(10);assertThat(snapshot.height()).isEqualTo(20);
  byte[] replacement=java.util.Base64.getDecoder().decode(snapshot.png().substring(snapshot.png().indexOf(',')+1));
  c.setAutoCommit(false);ImageAnnotationService.save(c,1,7,snapshot.revision(),replacement);c.commit();assertThat(ImageAnnotationService.dimensions(stored())).containsExactly(10,20);
 }
 @Test void malformedAndAnimatedPngsAreRejectedBeforeDecoding()throws Exception{
  assertThatThrownBy(()->ImageAnnotationService.normalize(new byte[40])).isInstanceOf(ResponseStatusException.class);
  byte[] png=picture(0,1,1);var out=new java.io.ByteArrayOutputStream();out.write(png,0,8);out.write(new byte[]{0,0,0,0,'a','c','T','L',0,0,0,0});out.write(png,8,png.length-8);
  assertThatThrownBy(()->ImageAnnotationService.dimensions(out.toByteArray())).isInstanceOf(ResponseStatusException.class);
 }
}
