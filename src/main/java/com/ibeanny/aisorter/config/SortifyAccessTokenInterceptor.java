package com.ibeanny.aisorter.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SortifyAccessTokenInterceptor implements HandlerInterceptor {
    public static final String ACCESS_TOKEN_HEADER = "X-Sortify-Access-Token";

    private final SortifySecurityProperties securityProperties;

    public SortifyAccessTokenInterceptor(SortifySecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!securityProperties.isAccessTokenRequired()) {
            return true;
        }

        String providedToken = request.getHeader(ACCESS_TOKEN_HEADER);
        if (providedToken != null && providedToken.equals(securityProperties.getAccessToken())) {
            return true;
        }

        response.sendError(HttpStatus.UNAUTHORIZED.value(), "A valid Sortify access token is required.");
        return false;
    }
}
