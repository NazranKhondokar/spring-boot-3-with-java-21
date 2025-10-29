package com.nazran.chat.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom implementation of {@link AuthenticationEntryPoint} for handling unauthorized access attempts.
 * This class is triggered when an unauthenticated user attempts to access a secure HTTP resource,
 * resulting in an {@link AuthenticationException}. It responds with an HTTP 401 Unauthorized status.
 */
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    /**
     * Handles the response when an unauthenticated user attempts to access a secured resource.
     * This method is invoked by Spring Security when an {@link AuthenticationException} occurs,
     * sending an HTTP 401 Unauthorized response with the exception message.
     *
     * @param request       the {@link HttpServletRequest} representing the incoming request
     * @param response      the {@link HttpServletResponse} to send the error response
     * @param authException the {@link AuthenticationException} that triggered this entry point
     * @throws IOException      if an input or output error occurs while writing the response
     * @throws ServletException if a servlet-specific error occurs during processing
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }
}