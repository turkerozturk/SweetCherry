package com.turkerozturk.export.exportedfiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportedFilePathsTest {
    @TempDir Path workingDirectory;

    @Test void resolvesOnlyExportsInsideConfiguredFolder() {
        assertThat(ExportedFilePaths.resolve(workingDirectory.toString(), "exportedFiles", "note.ctb"))
                .isEqualTo(workingDirectory.resolve("exportedFiles/note.ctb"));
        assertThat(ExportedFilePaths.resolve(workingDirectory.toString(), "exportedFiles", "note.ctb.old"))
                .isEqualTo(workingDirectory.resolve("exportedFiles/note.ctb.old"));
    }

    @Test void deletesExportWithParenthesizedTenantName() throws Exception {
        var folder=java.nio.file.Files.createDirectories(workingDirectory.resolve("exportedFiles"));
        String name="export_Exported Nodes (Read Only)_43.ctb";
        var file=java.nio.file.Files.writeString(folder.resolve(name),"export fixture");
        var controller=new FileDeleteController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller,"applicationPath",workingDirectory.toString());
        org.springframework.test.util.ReflectionTestUtils.setField(controller,"exportingFolderName","exportedFiles");
        assertThat(controller.deleteFile(name).getStatus().value()).isEqualTo(200);
        assertThat(java.nio.file.Files.exists(file)).isFalse();
    }

    @Test void acceptsNamesProducedFromTenantDisplayNames() {
        for (String name : new String[]{"export_Exported Nodes (Read Only)_43.ctb", "export_Türkçe & test (1)_43.ctb"})
            assertThat(ExportedFilePaths.resolve(workingDirectory.toString(), "exportedFiles", name))
                .isEqualTo(workingDirectory.resolve("exportedFiles").resolve(name));
    }

    @Test void rejectsTraversalAndUnsafeDownloadNames() {
        for (String name : new String[] {"../allTenants/demo.txt", "subdir/note.ctb", "note.ctb\\r\\nheader",
                "secret.txt", ".hidden.ctb", "C:\\secrets.ctb"}) {
            assertThatThrownBy(() -> ExportedFilePaths.resolve(workingDirectory.toString(), "exportedFiles", name))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> ExportedFilePaths.resolve(workingDirectory.toString(), "../outside", "note.ctb"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsAlternateSeparatorsAbsolutePathsAndWindowsAliasesBeforeFilesystemAccess() {
        for(String name:new String[]{"../note.ctb","..\\note.ctb","/note.ctb","C:note.ctb","note.ctb:stream","note.ctb ","note.ctb.","bad\n.ctb"})
            assertThatThrownBy(()->ExportedFilePaths.resolve(workingDirectory.toString(),"exportedFiles",name))
                    .isInstanceOf(IllegalArgumentException.class);
        for(String folder:new String[]{"../outside","..\\outside","/outside","C:\\outside","exports.","exports "})
            assertThatThrownBy(()->ExportedFilePaths.folder(workingDirectory.toString(),folder))
                    .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void symlinkedFolderIsRejectedByBothWriterAndReaderPaths() throws Exception {
        var outside=java.nio.file.Files.createDirectory(workingDirectory.resolve("outside"));
        var folder=workingDirectory.resolve("exports");symlink(folder,outside);
        assertThatThrownBy(()->ExportedFilePaths.folder(workingDirectory.toString(),"exports")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->ExportedFilePaths.resolve(folder,"note.ctb")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void symlinkedFileCannotBeDownloadedDeletedOrOpenedForCollectionExport() throws Exception {
        var outside=java.nio.file.Files.writeString(workingDirectory.resolve("outside.ctb"),"unchanged");
        var folder=java.nio.file.Files.createDirectory(workingDirectory.resolve("exports"));
        symlink(folder.resolve("exportednodes.ctb"),outside);
        assertThatThrownBy(()->ExportedFilePaths.resolve(folder,"exportednodes.ctb")).isInstanceOf(IllegalArgumentException.class);
        var download=new FileDownloadController();var delete=new FileDeleteController();
        for(Object controller:new Object[]{download,delete}) {
            org.springframework.test.util.ReflectionTestUtils.setField(controller,"applicationPath",workingDirectory.toString());
            org.springframework.test.util.ReflectionTestUtils.setField(controller,"exportingFolderName","exports");
        }
        assertThat(download.downloadFile("exportednodes.ctb").getStatusCode().value()).isEqualTo(400);
        assertThat(delete.deleteFile("exportednodes.ctb").getStatus().value()).isEqualTo(400);
        var export=new com.turkerozturk.export.CtbExportService();
        org.springframework.test.util.ReflectionTestUtils.setField(export,"applicationPath",workingDirectory.toString());
        org.springframework.test.util.ReflectionTestUtils.setField(export,"exportingFolderName","exports");
        com.turkerozturk.multipledatabases.TenantContext.setCurrentTenant("Test");
        try {assertThatThrownBy(()->export.export(1,true,true)).isInstanceOf(IllegalArgumentException.class);}
        finally {com.turkerozturk.multipledatabases.TenantContext.clear();}
        assertThat(java.nio.file.Files.readString(outside)).isEqualTo("unchanged");
    }
    private void symlink(Path link,Path target) throws Exception {
        try {java.nio.file.Files.createSymbolicLink(link,target);}
        catch(UnsupportedOperationException|java.nio.file.FileSystemException unsupported) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,"Symbolic links require filesystem support and permission");
        }
    }
}
