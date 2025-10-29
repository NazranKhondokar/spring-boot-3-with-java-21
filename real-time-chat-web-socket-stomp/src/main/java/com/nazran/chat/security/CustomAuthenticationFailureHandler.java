package com.nazran.chat.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * Custom implementation of {@link AuthenticationFailureHandler} that sends
 * an HTTP 401 (Unauthorized) response with the authentication failure message
 * when authentication fails.
 */
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /**
     * Handles an authentication failure by sending an HTTP 401 (Unauthorized) response
     * with the exception message as the response body.
     *
     * @param httpServletRequest  the request during which the authentication attempt occurred
     * @param httpServletResponse the response to be sent to the client
     * @param e                   the exception which caused the authentication failure
     * @throws IOException if an input or output exception occurs
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest,
                                        HttpServletResponse httpServletResponse,
                                        AuthenticationException e) throws IOException {
        httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpServletResponse.getWriter().write("Authentication Failed: " + e.getMessage());
        httpServletResponse.getWriter().flush();
    }
}