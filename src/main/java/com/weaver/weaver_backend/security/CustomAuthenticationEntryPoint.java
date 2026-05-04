package com.weaver.weaver_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weaver.weaver_backend.dto.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        logger.error("Unauthorized error: {}", authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        String authHeader = request.getHeader("Authorization");
        String message;
        // No token provided
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            message = "Please login";
        else
            message = "Invalid or expired token";

        ApiResponse<?> apiResponse = ApiResponse.error(
                401,
                HttpStatus.UNAUTHORIZED,
                message
        );

        response.getWriter().write(
                objectMapper.writeValueAsString(apiResponse));
    }

}

