package com.nazran.chat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.nazran.chat.utils.ResponseStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication filter for Firebase JWT token verification.
 * This filter intercepts incoming requests and validates Firebase ID tokens
 * in the Authorization header.
 */
@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);

    /**
     * Processes each HTTP request to verify Firebase authentication token.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during request processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = null;

        // 1. Try to get token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2. If no token in header, try to get it from cookie named 'idToken'
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("idToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            logger.info("Verifying Firebase token for request: {}", request.getRequestURI());

            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                String uid = decodedToken.getUid();
                logger.info("Token successfully verified for user: {}", uid);

                // Set Authentication for Spring Security context
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(uid, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (FirebaseAuthException e) {
                logger.error("Token verification failed: {}", e.getMessage());

                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("status", ResponseStatus.ERROR);
                responseBody.put("message", "Invalid token");
                responseBody.put("errors", e.getMessage());

                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(response.getOutputStream(), responseBody);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}