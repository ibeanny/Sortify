package com.ibeanny.aisorter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sortify.upload")
public class UploadLimitsProperties {
    private int maxFiles = 10;
    private long maxFileSizeBytes = 1024 * 1024;
    private long maxTotalUploadBytes = 4L * 1024 * 1024;

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public long getMaxTotalUploadBytes() {
        return maxTotalUploadBytes;
    }

    public void setMaxTotalUploadBytes(long maxTotalUploadBytes) {
        this.maxTotalUploadBytes = maxTotalUploadBytes;
    }
}
