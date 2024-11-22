package com.nazran.jwtrbacsecurity.controller;

import com.nazran.jwtrbacsecurity.security.JwtGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtGenerator jwtGenerator;

    public AuthController(JwtGenerator jwtGenerator) {
        this.jwtGenerator = jwtGenerator;
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (!jwtGenerator.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }

        String username = jwtGenerator.getUsernameFromToken(refreshToken);
        Map<String, Object> claims = new HashMap<>();
        String newAccessToken = jwtGenerator.generateAccessToken(username, claims);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
}

