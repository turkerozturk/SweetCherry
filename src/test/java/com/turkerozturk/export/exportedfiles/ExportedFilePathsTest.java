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
        assertThat(controller.deleteFile(name).getStatusCode().value()).isEqualTo(200);
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
}
