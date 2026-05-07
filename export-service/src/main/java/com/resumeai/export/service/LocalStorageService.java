package com.resumeai.export.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class LocalStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    @Value("${export.local-storage-dir}")
    private String storageDir;

    public String storeFile(File sourceFile, String targetFileName) throws Exception {
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Path targetPath = Path.of(storageDir, targetFileName);
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored export file at {}", targetPath);
        return targetPath.toString();
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
            log.info("Deleted export file {}", filePath);
        } catch (Exception ex) {
            log.warn("Failed to delete export file {} reason={}", filePath, ex.getMessage());
        }
    }
}
