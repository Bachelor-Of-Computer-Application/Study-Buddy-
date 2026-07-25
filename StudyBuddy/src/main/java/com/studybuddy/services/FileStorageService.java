package com.studybuddy.services;

import com.studybuddy.admin.services.SettingsService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Centralized file storage: copy uploads to configured directory,
 * validate size/type, generate unique names, delete on content removal.
 */
public class FileStorageService {

    private static final Logger logger = Logger.getLogger(FileStorageService.class.getName());
    private static FileStorageService instance;

    private final SettingsService settingsService = SettingsService.getInstance();

    private FileStorageService() {}

    public static synchronized FileStorageService getInstance() {
        if (instance == null) {
            instance = new FileStorageService();
        }
        return instance;
    }

    public Path getStorageRoot() throws IOException {
        String configured = settingsService.getSetting("storage_directory", "").trim();
        Path root = !configured.isEmpty()
                ? Path.of(configured)
                : Path.of(System.getProperty("user.home"), "StudyBuddy", "uploads");
        Files.createDirectories(root);
        return root;
    }

    public long getMaxUploadBytes() {
        try {
            int mb = Integer.parseInt(settingsService.getSetting("max_upload_size_mb", "50"));
            return (long) mb * 1024 * 1024;
        } catch (NumberFormatException e) {
            return 50L * 1024 * 1024;
        }
    }

    public Set<String> getAllowedExtensions() {
        String raw = settingsService.getSetting("allowed_file_types",
                "pdf,docx,pptx,ppt,zip,xlsx,jpg,png,jpeg");
        return Arrays.stream(raw.toLowerCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public void validateFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("No file selected.");
        }
        if (file.length() > getMaxUploadBytes()) {
            throw new IOException("File exceeds maximum upload size ("
                    + settingsService.getSetting("max_upload_size_mb", "50") + " MB).");
        }
        String ext = extension(file.getName());
        if (!getAllowedExtensions().contains(ext)) {
            throw new IOException("File type '." + ext + "' is not allowed.");
        }
    }

    public String storeFile(File source, String subfolder) throws IOException {
        validateFile(source);
        Path root = getStorageRoot();
        Path targetDir = subfolder != null && !subfolder.isBlank() ? root.resolve(subfolder) : root;
        Files.createDirectories(targetDir);
        String uniqueName = UUID.randomUUID() + "_" + sanitizeFilename(source.getName());
        Path target = targetDir.resolve(uniqueName);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Stored file: " + target);
        return target.toAbsolutePath().toString();
    }

    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return true;
        }
        try {
            Path path = Path.of(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
            }
            return true;
        } catch (IOException e) {
            logger.warning("Failed to delete file " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
