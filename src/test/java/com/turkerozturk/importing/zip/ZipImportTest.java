package com.turkerozturk.importing.zip;

import com.turkerozturk.helpers.*;
import com.turkerozturk.node.properties.NodePropertiesService;
import com.turkerozturk.richtext.editing.EmbeddedUploadPolicy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZipImportTest {
    @TempDir Path directory;
    static final ZipImportData.Options ALL=new ZipImportData.Options(false,true,true,"UTF-8");
    Path zip(Map<String,byte[]> files)throws Exception {
        Path path=directory.resolve(UUID.randomUUID()+".zip");
        try(var out=new ZipOutputStream(Files.newOutputStream(path))){
            for(var item:files.entrySet()){
                var entry=new ZipEntry(item.getKey());entry.setLastModifiedTime(java.nio.file.attribute.FileTime.from(java.time.Instant.parse("2020-01-02T03:04:06Z")));
                out.putNextEntry(entry);out.write(item.getValue());out.closeEntry();
            }
        }return path;
    }
    static byte[] text(String text){return text.getBytes(StandardCharsets.UTF_8);}
    ZipImportData read(Map<String,byte[]> files)throws Exception{return ZipImportData.read(zip(files),ALL,9_000_000);}
    Connection target()throws Exception {
        var c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve(UUID.randomUUID()+".ctb"));
        try(var s=c.createStatement();var in=getClass().getResourceAsStream("/schemas/schema-sqlite.sql")){
            for(String sql:new String(in.readAllBytes(),StandardCharsets.UTF_8).split(";"))if(!sql.isBlank())s.execute(sql);
            s.execute("INSERT INTO node VALUES(80,'Existing','unchanged','plain-text','',0,0,0,0,0,0,1,2)");
            s.execute("INSERT INTO children VALUES(80,0,7,0)");s.execute("INSERT INTO children VALUES(100,80,1,80)");
        }c.setAutoCommit(false);return c;
    }
    long number(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    String string(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getString(1);}}
    @Test void createsMissingFoldersAndUsesCanonicalSyntaxAndIconsWithoutChangingText()throws Exception {
        var files=new LinkedHashMap<String,byte[]>();files.put("project/readme.MD",text("# Title\nTürkçe 😀\n"));files.put("project/main.py",text("print('x')"));files.put("project/page.html",text("<h1>HTML</h1>"));files.put("unknown.abc",text("ordinary"));files.put("empty/",new byte[0]);
        var data=read(files);
        assertThat(data.count()).isEqualTo(6);assertThat(data.entries().get(0).action()).isEqualTo(ZipImportData.Action.DIRECTORY);
        assertThat(data.entries().get(1).parent()).isZero();assertThat(data.entries().get(1).syntax()).isEqualTo("markdown");
        assertThat(data.entries().get(1).text()).isEqualTo("# Title\nTürkçe 😀\n");assertThat(data.entries().get(1).icon()).isEqualTo(NodeIcon.CT_MARKDOWN.getIconIdIn16bit());
        assertThat(data.entries().get(2).syntax()).isEqualTo("python");assertThat(data.entries().get(3).syntax()).isEqualTo("html");
        assertThat(data.entries().get(4).syntax()).isEqualTo("plain-text");assertThat(data.entries().get(4).icon()).isEqualTo(NodeIcon.CT_NOTES.getIconIdIn16bit());
        try(var c=target()) {
            assertThat(data.insert(c)).isEqualTo(6);
            assertThat(string(c,"SELECT txt FROM node WHERE node_id=80")).isEqualTo("unchanged");
            assertThat(number(c,"SELECT father_id FROM children WHERE node_id=102")).isEqualTo(101);
            assertThat(number(c,"SELECT sequence FROM children WHERE node_id=101")).isEqualTo(8);
            assertThat(number(c,"SELECT sequence FROM children WHERE node_id=105")).isEqualTo(9);
        }
    }
    @Test void attachmentRowsUseCherryTreeLayoutAndKeepOriginalFilename()throws Exception {
        byte[] payload={0,1,2,3,4};var path=zip(Map.of("folder/a.bin",payload));
        var data=ZipImportData.read(path,new ZipImportData.Options(true,true,true,"UTF-8"),100);
        assertThat(data.entries().get(1).name()).isEqualTo("a");
        try(var c=target()) {
            data.insert(c);assertThat(string(c,"SELECT syntax FROM node WHERE node_id=102")).isEqualTo("custom-colors");
            assertThat(number(c,"SELECT is_richtxt FROM node WHERE node_id=102")).isEqualTo(1);
            assertThat(number(c,"SELECT is_ro FROM node WHERE node_id=102")).isEqualTo((long)NodeIcon.CT_FILE.getIconIdIn16bit());
            assertThat(number(c,"SELECT has_image FROM node WHERE node_id=102")).isEqualTo(1);
            assertThat(string(c,"SELECT filename FROM image")).isEqualTo("a.bin");assertThat(number(c,"SELECT \"offset\" FROM image")).isZero();
            try(var s=c.createStatement();var r=s.executeQuery("SELECT png FROM image")){r.next();assertThat(r.getBytes(1)).isEqualTo(payload);}
        }
    }
    @Test void optionsSkipBinariesOrKeepLargeFileMetadataWithFullNameAndUtcDates()throws Exception {
        Path archive=zip(Map.of("large.bin",new byte[ZipImportData.TEXT_BYTES+1],"small.bin",new byte[]{0,1},"note.txt",text("hello")));
        var data=ZipImportData.read(archive,new ZipImportData.Options(true,false,true,"UTF-8"),100);
        var large=data.entries().stream().filter(e->e.path().equals("large.bin")).findFirst().orElseThrow();
        assertThat(large.name()).isEqualTo("large.bin");assertThat(large.syntax()).isEqualTo("plain-text");assertThat(large.icon()).isEqualTo(NodeIcon.CT_CANCEL.getIconIdIn16bit());
        assertThat(large.text()).contains("2000001 bytes","2020-01-02 03:04:06","Created (UTC): Not available in ZIP");
        assertThat(data.count("SKIP")).isEqualTo(1);assertThat(data.count()).isEqualTo(2);
        var skipped=ZipImportData.read(archive,new ZipImportData.Options(false,false,false,"UTF-8"),100);
        assertThat(skipped.count()).isEqualTo(1);assertThat(skipped.entries().stream().filter(e->e.action()==ZipImportData.Action.TEXT).findFirst().orElseThrow().name()).isEqualTo("note.txt");
    }
    @Test void detectsBinaryContentEvenWithCodeExtensionAndSupportsBomAndLegacyText()throws Exception {
        var data=read(Map.of("fake.md",new byte[]{0,1,2},"unicode.txt",new byte[]{(byte)0xff,(byte)0xfe,0x41,0,0x42,0},"x.pdf",text("%PDF-1.7")));
        assertThat(data.count("ATTACHMENT")).isEqualTo(2);assertThat(data.entries().stream().filter(e->e.path().equals("unicode.txt")).findFirst().orElseThrow().text()).isEqualTo("AB");
        Path legacy=zip(Map.of("tr.txt","şğü".getBytes(Charset.forName("windows-1254"))));
        assertThat(ZipImportData.read(legacy,new ZipImportData.Options(false,false,false,"windows-1254"),100).entries().get(0).text()).isEqualTo("şğü");
        assertThat(ZipImportData.read(legacy,ALL,100).count("ATTACHMENT")).isEqualTo(1);
    }
    @Test void rejectsTraversalAmbiguousNamesCollisionsTooManyEntriesAndDepth()throws Exception {
        for(String name:List.of("../outside","/absolute","C:/drive","a\\b","a/../b","a//b","x\u0001","x".repeat(256)))
            assertThatThrownBy(()->read(Map.of(name,text("bad")))).hasMessage("path");
        assertThatThrownBy(()->read(new LinkedHashMap<>(Map.of("a",text("file"),"a/b",text("child"))))).hasMessage("path");
        assertThatThrownBy(()->read(Map.of("a/".repeat(65)+"file",text("x")))).hasMessage("limit");
        var many=new LinkedHashMap<String,byte[]>();for(int i=0;i<10001;i++)many.put("f"+i,new byte[0]);assertThatThrownBy(()->read(many)).hasMessage("limit");
        assertThatThrownBy(()->read(Map.of())).hasMessage("empty");
        Path notZip=directory.resolve("bad.zip");Files.writeString(notZip,"not a ZIP");assertThatThrownBy(()->ZipImportData.read(notZip,ALL,100)).hasMessage("zip");
    }
    @Test void retainedContentBudgetStopsHighlyCompressedArchives()throws Exception {
        var files=new LinkedHashMap<String,byte[]>();byte[] text=new byte[ZipImportData.TEXT_BYTES];Arrays.fill(text,(byte)'a');for(int i=0;i<10;i++)files.put(i+".txt",text);
        assertThatThrownBy(()->read(files)).hasMessage("limit");
    }
    @Test void uploadLimitAndWritableAndChangedAttachmentPolicyAreRechecked()throws Exception {
        var properties=mock(NodePropertiesService.class);var policy=mock(EmbeddedUploadPolicy.class);when(policy.fileBytes()).thenReturn(100);
        var multipart=new MultipartProperties();multipart.setMaxFileSize(org.springframework.util.unit.DataSize.ofBytes(5));
        var service=new ZipImportService(properties,policy,multipart);
        assertThatThrownBy(()->service.preview(new java.io.ByteArrayInputStream(new byte[0]),ALL)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        when(properties.writable()).thenReturn(true);
        assertThatThrownBy(()->service.preview(new java.io.ByteArrayInputStream(new byte[6]),ALL)).hasMessage("upload");
        var data=read(Map.of("b.bin",new byte[]{0,1,2}));when(properties.writable()).thenReturn(false);
        assertThatThrownBy(()->service.confirm(data)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        when(properties.writable()).thenReturn(true);when(policy.fileBytes()).thenReturn(1);assertThatThrownBy(()->service.confirm(data)).hasMessage("changed");
    }
    @Test void overflowAndNonTransactionalConnectionsAreRejectedBeforeWrites()throws Exception {
        try(var c=target()) {try(var s=c.createStatement()){s.execute("INSERT INTO children VALUES(9223372036854775807,0,99,80)");}
            assertThatThrownBy(()->read(Map.of("a.txt",text("hello"))).insert(c)).isInstanceOf(ArithmeticException.class);assertThat(number(c,"SELECT COUNT(*) FROM node")).isEqualTo(1);}
        try(var c=target()){c.commit();c.setAutoCommit(true);assertThatThrownBy(()->read(Map.of("a.txt",text("hello"))).insert(c)).isInstanceOf(SQLException.class);}
    }
    @Test void failedInsertRollsBackNodesHierarchyAndAttachmentsTogether()throws Exception {
        try(var c=target()) {
            try(var statement=c.createStatement()){statement.execute("CREATE TRIGGER fail_second BEFORE INSERT ON children WHEN NEW.node_id>101 BEGIN SELECT RAISE(ABORT,'test failure'); END");}c.commit();
            var properties=mock(NodePropertiesService.class);when(properties.writable()).thenReturn(true);
            var policy=mock(EmbeddedUploadPolicy.class);when(policy.fileBytes()).thenReturn(100);
            var raw=new ZipImportService(properties,policy,new MultipartProperties());var em=mock(jakarta.persistence.EntityManager.class);var session=mock(org.hibernate.Session.class);
            when(em.unwrap(org.hibernate.Session.class)).thenReturn(session);ReflectionTestUtils.setField(raw,"entityManager",em);
            var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource(c.getMetaData().getURL());
            when(session.doReturningWork(any())).thenAnswer(call->{var tx=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(ds);try{return ((org.hibernate.jdbc.ReturningWork<?>)call.getArgument(0)).execute(tx);}finally{org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(tx,ds);}});
            var proxy=new org.springframework.aop.framework.ProxyFactory(raw);
            proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
            var service=(ZipImportService)proxy.getProxy();var files=new LinkedHashMap<String,byte[]>();files.put("a.bin",new byte[]{0,1});files.put("b.txt",text("text"));
            var data=read(files);assertThatThrownBy(()->service.confirm(data)).hasRootCauseMessage("[SQLITE_CONSTRAINT_TRIGGER] A RAISE function within a trigger fired, causing the SQL statement to abort (test failure)");
            assertThat(number(c,"SELECT COUNT(*) FROM node")).isEqualTo(1);assertThat(number(c,"SELECT COUNT(*) FROM children")).isEqualTo(2);assertThat(number(c,"SELECT COUNT(*) FROM image")).isZero();
        }
    }
    @Test void crcDamageIsNotSilentlyImported()throws Exception {
        Path file=directory.resolve("crc.zip");byte[] payload=text("original unique content");var crc=new CRC32();crc.update(payload);
        try(var out=new ZipOutputStream(Files.newOutputStream(file))){var e=new ZipEntry("x.txt");e.setMethod(ZipEntry.STORED);e.setSize(payload.length);e.setCrc(crc.getValue());out.putNextEntry(e);out.write(payload);out.closeEntry();}
        byte[] bytes=Files.readAllBytes(file);int start=30+5;bytes[start]^=1;Files.write(file,bytes);
        assertThatThrownBy(()->ZipImportData.read(file,ALL,100)).hasMessage("zip");
    }
    @Test void canonicalExtensionsAreUnambiguousAndCaseInsensitive(){
        assertThat(CodeIcon.getCodeIconByFileExtension(".MD")).isEqualTo(CodeIcon.MARKDOWN);
        assertThat(CodeIcon.getCodeIconByFileExtension("py")).isEqualTo(CodeIcon.PYTHON);
        assertThat(CodeIcon.getCodeIconByFileExtension("htm")).isEqualTo(CodeIcon.HTML);
        assertThat(CodeIcon.findByFileExtension("unknown")).isEmpty();assertThat(CodeIcon.findByFileExtension(null)).isEmpty();
        var extensions=Arrays.stream(CodeIcon.values()).map(CodeIcon::getFileExtension).filter(e->!e.isEmpty()).toList();assertThat(extensions).doesNotHaveDuplicates();
    }
}
