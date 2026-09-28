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
