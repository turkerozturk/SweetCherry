package com.turkerozturk.export.exportedfiles;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;

/** One boundary for export writers, downloads and deletions. Configuration names are single segments. */
public final class ExportedFilePaths {
    private ExportedFilePaths() { }

    public static Path folder(String applicationPath, String exportingFolderName) {
        if (applicationPath == null || exportingFolderName == null
                || !exportingFolderName.matches("[\\p{L}\\p{N}._ -]+")
                || exportingFolderName.startsWith(".") || exportingFolderName.endsWith(" ")
                || exportingFolderName.endsWith("."))
            throw new IllegalArgumentException("Invalid export folder");
        Path base = Path.of(applicationPath).toAbsolutePath().normalize();
        Path folder = base.resolve(exportingFolderName).normalize();
        if (!folder.startsWith(base) || !base.equals(folder.getParent())
                || Files.isSymbolicLink(folder)
                || Files.exists(folder, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Export folder must be a plain directory inside the application folder");
        return folder;
    }

    public static Path resolve(String applicationPath, String exportingFolderName, String filename) {
        return resolve(folder(applicationPath, exportingFolderName), filename);
    }

    public static Path resolve(Path folder, String filename) {
        if (filename == null || !filename.matches("[^\\\\/:*?\"<>|\\p{Cntrl}]+")
                || filename.startsWith(".") || filename.endsWith(" ") || filename.endsWith("."))
            throw new IllegalArgumentException("Invalid export filename");
        String lowerName = filename.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".ctb") && !lowerName.endsWith(".old"))
            throw new IllegalArgumentException("Unsupported export file");
        Path base = folder.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(base))throw new IllegalArgumentException("Export folder must not be a symbolic link");
        Path file = base.resolve(filename).normalize();
        if (!file.startsWith(base) || !base.equals(file.getParent()))
            throw new IllegalArgumentException("Export file is outside the output folder");
        requirePlainFile(file);
        return file;
    }

    /** Missing destinations are allowed; existing symlinks and directories never are. */
    public static void requirePlainFile(Path file) {
        if (Files.isSymbolicLink(file)
                || Files.exists(file, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Export file must not be a symbolic link or directory");
    }
}
