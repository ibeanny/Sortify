package com.ibeanny.aisorter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sortify.security")
public class SortifySecurityProperties {
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isAccessTokenRequired() {
        return accessToken != null && !accessToken.isBlank();
    }
}
