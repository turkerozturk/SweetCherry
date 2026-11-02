package com.turkerozturk.export.texttree;

import com.turkerozturk.exportmindmap.MindMapTextContent;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TextTreeExportTest {
    private final TextTreeExportLimits limits=new TextTreeExportLimits();
    private final MindMapTextContent text=mock(MindMapTextContent.class);
    private final TextTreeExportService service=new TextTreeExportService(text,limits);
    private Connection database()throws Exception{
        var c=DriverManager.getConnection("jdbc:sqlite::memory:");
        try(var s=c.createStatement()){
            s.executeUpdate(Files.readString(Path.of("src/test/resources/fixtures/shared-node-tree.sql")));
        }return c;
    }
    private Map<String,String> entries(Path file)throws Exception{
        var result=new LinkedHashMap<String,String>();try(var zip=new ZipFile(file.toFile(),StandardCharsets.UTF_8)){
            for(var entry:Collections.list(zip.entries()))result.put(entry.getName(),new String(zip.getInputStream(entry).readAllBytes(),StandardCharsets.UTF_8));
        }return result;
    }
    @Test void exportsFoldersBranchContentAndExactLeafTextWithOriginalExtensionsWithoutWritingSource()throws Exception{
        try(var c=database();var s=c.createStatement()){
            s.execute("UPDATE node SET name='Playbooks',txt='Parent note' WHERE node_id=2");
            String original="---\r\n# Türkçe 😀\r\n- hosts: all\r\n";
            try(var q=c.prepareStatement("UPDATE node SET name='add_user.ansible',txt=?,syntax='yaml' WHERE node_id=3")){q.setString(1,original);q.executeUpdate();}
            s.execute("PRAGMA query_only=ON");Path path;
            try(var archive=service.create(c,2,true)){
                path=archive.path();assertThat(archive.nodes()).isEqualTo(2);
                assertThat(entries(path)).containsExactly(entry("Playbooks/",""),entry("Playbooks/_content.txt","Parent note"),entry("Playbooks/add_user.ansible",original));
            }assertThat(path).doesNotExist();verifyNoInteractions(text);
            try(var r=s.executeQuery("SELECT COUNT(*) FROM children")){r.next();assertThat(r.getInt(1)).isEqualTo(19);}
        }
    }
    @Test void sharedOccurrenceKeepsOwnSubtreeAndReadsMasterWithoutBorrowingMastersOtherChildren()throws Exception{
        try(var c=database();var archive=service.create(c,11,true)){
            var result=entries(archive.path());
            assertThat(result.keySet()).anyMatch(n->n.contains("Node 12")).anyMatch(n->n.contains("Node 19"));
            assertThat(result.keySet()).noneMatch(n->n.contains("Node 3"));
            assertThat(result.get("Node 2/_content.txt")).contains("master 2");
        }
    }
    @Test void singleNodeAndMarkdownPreserveSourceAndDotfiles()throws Exception{
        try(var c=database();var s=c.createStatement()){
            s.execute("UPDATE node SET name='.env',txt='# source **literal**',syntax='markdown' WHERE node_id=2");
            try(var archive=service.create(c,2,false)){assertThat(entries(archive.path())).containsExactly(entry(".env","# source **literal**"));}
            verifyNoInteractions(text);
        }
    }
    @Test void reservedContentFilenameDoesNotOverwriteChildAndEmptyBranchHasNoExtraContentFile()throws Exception{
        try(var c=database();var s=c.createStatement()){
            s.execute("UPDATE node SET txt='',name='Root' WHERE node_id=2");s.execute("UPDATE node SET name='_content.txt',txt='child' WHERE node_id=3");
            try(var archive=service.create(c,2,true)){assertThat(entries(archive.path())).containsExactly(entry("Root/",""),entry("Root/_content--3.txt","child"));}
        }
    }
    @Test void flattensRichTextThroughExistingObjectAwareConverter()throws Exception{
        try(var c=database();var s=c.createStatement()){
            s.execute("UPDATE node SET syntax='custom-colors',txt='<node><rich_text>hello</rich_text></node>' WHERE node_id=2");
            when(text.text(eq(2L),eq("custom-colors"),anyString())).thenReturn("hello (https://example.org)\n[Image]\nA\tB");
            try(var archive=service.create(c,2,false)){assertThat(entries(archive.path()).values()).containsExactly("hello (https://example.org)\n[Image]\nA\tB");}
            verify(text).text(eq(2L),eq("custom-colors"),anyString());
        }
    }
    @Test void refusesCyclesMissingMastersAndNodeDepthAndByteLimits()throws Exception{
        try(var c=database();var s=c.createStatement()){
            limits.setMaxNodes(1);assertThatThrownBy(()->service.create(c,2,true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("413");
            limits.setMaxNodes(100);limits.setMaxTextBytes(1);assertThatThrownBy(()->service.create(c,2,false)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("413");
            limits.setMaxTextBytes(100000);limits.setMaxNodeCharacters(1);assertThatThrownBy(()->service.create(c,2,false)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("413");
            limits.setMaxNodeCharacters(100000);limits.setMaxDepth(1);assertThatThrownBy(()->service.create(c,11,true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("413");
            limits.setMaxDepth(64);s.execute("UPDATE children SET master_id=99999 WHERE node_id=11");assertThatThrownBy(()->service.create(c,11,true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");
            s.execute("UPDATE children SET father_id=3 WHERE node_id=2");assertThatThrownBy(()->service.create(c,2,true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("409");
        }
    }
    @Test void portableNamesAvoidTraversalCaseCollisionsReservedDevicesAndOverlongUnicode(){
        var paths=new TextTreePaths();
        assertThat(TextTreePaths.safe("COM¹",9)).startsWith("node-9-");
        assertThat(paths.allocate("","Test",1,false)).isEqualTo("Test.txt");assertThat(paths.allocate("","test",2,false)).isEqualTo("test--2.txt");
        for(String name:List.of("../../a","C:\\secret\\name","CON","NUL.txt","..","", "汉".repeat(300))){
            String value=paths.allocate("",name,9,false);assertThat(value).doesNotContain("/","\\",":");assertThat(value).isNotIn(".","..");assertThat(value.getBytes(StandardCharsets.UTF_8).length).isLessThan(256);
        }
    }
}
