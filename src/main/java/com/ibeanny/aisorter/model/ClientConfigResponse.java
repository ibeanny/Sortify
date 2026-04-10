package com.ibeanny.aisorter.model;

public class ClientConfigResponse {
    private boolean accessTokenRequired;
    private int maxFiles;
    private long maxFileSizeBytes;
    private long maxTotalUploadBytes;

    public ClientConfigResponse() {
    }

    public ClientConfigResponse(boolean accessTokenRequired, int maxFiles, long maxFileSizeBytes, long maxTotalUploadBytes) {
        this.accessTokenRequired = accessTokenRequired;
        this.maxFiles = maxFiles;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxTotalUploadBytes = maxTotalUploadBytes;
    }

    public boolean isAccessTokenRequired() {
        return accessTokenRequired;
    }

    public void setAccessTokenRequired(boolean accessTokenRequired) {
        this.accessTokenRequired = accessTokenRequired;
    }

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
