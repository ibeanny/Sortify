package com.ibeanny.aisorter.service;

import com.ibeanny.aisorter.config.UploadLimitsProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadValidationService {
    private final UploadLimitsProperties uploadLimitsProperties;

    public UploadValidationService(UploadLimitsProperties uploadLimitsProperties) {
        this.uploadLimitsProperties = uploadLimitsProperties;
    }

    public void validateFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Please upload at least one .txt file.");
        }
        if (files.length > uploadLimitsProperties.getMaxFiles()) {
            throw new IllegalArgumentException("You can upload up to %d files at a time.".formatted(uploadLimitsProperties.getMaxFiles()));
        }

        long totalBytes = 0L;
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
                throw new IllegalArgumentException("Only .txt files are allowed.");
            }
            if (file.getSize() > uploadLimitsProperties.getMaxFileSizeBytes()) {
                throw new IllegalArgumentException(
                        "Each file must be %s or smaller.".formatted(formatBytes(uploadLimitsProperties.getMaxFileSizeBytes()))
                );
            }
            totalBytes += file.getSize();
        }

        if (totalBytes > uploadLimitsProperties.getMaxTotalUploadBytes()) {
            throw new IllegalArgumentException(
                    "The total upload size must be %s or smaller.".formatted(formatBytes(uploadLimitsProperties.getMaxTotalUploadBytes()))
            );
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024) {
            return "%d MB".formatted(bytes / (1024 * 1024));
        }
        if (bytes >= 1024) {
            return "%d KB".formatted(bytes / 1024);
        }
        return "%d bytes".formatted(bytes);
    }
}
