package com.ibeanny.aisorter.model;

import java.util.List;

public class ClientConfigResponse {
    private boolean accessTokenRequired;
    private int maxFiles;
    private long maxFileSizeBytes;
    private long maxTotalUploadBytes;
    private List<String> allowedCategories;

    public ClientConfigResponse() {
    }

    public ClientConfigResponse(boolean accessTokenRequired, int maxFiles, long maxFileSizeBytes, long maxTotalUploadBytes, List<String> allowedCategories) {
        this.accessTokenRequired = accessTokenRequired;
        this.maxFiles = maxFiles;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxTotalUploadBytes = maxTotalUploadBytes;
        this.allowedCategories = allowedCategories;
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

    public List<String> getAllowedCategories() {
        return allowedCategories;
    }

    public void setAllowedCategories(List<String> allowedCategories) {
        this.allowedCategories = allowedCategories;
    }
}
