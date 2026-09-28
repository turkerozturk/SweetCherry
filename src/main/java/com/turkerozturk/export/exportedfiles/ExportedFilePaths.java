package com.turkerozturk.export.exportedfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Resolves export downloads and deletions within the configured output folder. */
final class ExportedFilePaths {
    private ExportedFilePaths() {
    }

    static Path resolve(String applicationPath, String exportingFolderName, String filename) {
        if (applicationPath == null || exportingFolderName == null || filename == null
                || !exportingFolderName.matches("[\\p{L}\\p{N}._ -]+")
                || exportingFolderName.startsWith(".")
                || !filename.matches("[\\p{L}\\p{N}._ -]+")
                || filename.startsWith(".")) {
            throw new IllegalArgumentException("Invalid export path");
        }
        String lowerName = filename.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".ctb") && !lowerName.endsWith(".old")) {
            throw new IllegalArgumentException("Unsupported export file");
        }
        Path folder = Path.of(applicationPath).toAbsolutePath().normalize().resolve(exportingFolderName);
        if (Files.isSymbolicLink(folder)) {
            throw new IllegalArgumentException("Export folder must not be a symbolic link");
        }
        return folder.resolve(filename);
    }
}
