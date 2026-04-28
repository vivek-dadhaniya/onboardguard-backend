package com.onboardguard.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Returns a JSON 401 body instead of an HTML error page.
 * Angular's HTTP interceptor reads response.status === 401 and redirects to login.
 *
 * Response body:
 * {
 *   "status": 401,
 *   "error": "Unauthorized",
 *   "message": "...",
 *   "path": "/api/v1/...",
 *   "timestamp": "2024-..."
 * }
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "Authentication failed");
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now());

        objectMapper.writeValue(response.getOutputStream(), body);


    }
}