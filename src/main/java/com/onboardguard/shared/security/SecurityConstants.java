package com.onboardguard.shared.security;


public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/login/candidate",
            "/api/v1/auth/login/staff",
            "/api/v1/auth/register/candidate",
            "/actuator/health"
    };
}