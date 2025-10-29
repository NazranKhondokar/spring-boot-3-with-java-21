package com.nazran.chat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nazran.chat.utils.ResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        logger.warn("Unauthorized access attempt: {} {} from IP: {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", ResponseStatus.ERROR);
        responseBody.put("message", "Authentication failed");
        responseBody.put("errors", authException.getMessage());
        responseBody.put("path", request.getRequestURI());
        responseBody.put("method", request.getMethod());

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), responseBody);
    }
}