package com.nazran.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Component responsible for generating, parsing, and validating JSON Web Tokens (JWTs).
 * This class uses a secret key and configurable expiration times to create access and refresh tokens,
 * and provides methods to extract claims and validate token integrity.
 */
@Component
public class JwtGenerator {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration}")
    private long accessTokenExpire;

    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpire;

    /**
     * Generates a JWT token with the specified authentication, user type, and expiration time.
     *
     * @param authentication the {@link Authentication} object containing user details
     * @param userType       the type of user to include as a claim in the token
     * @param expireTime     the expiration time in milliseconds from the current time
     * @return the generated JWT token as a compact string
     */
    public String generateToken(Authentication authentication, Long userType, Long expireTime) {
        String username = authentication.getName();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .claim("usertype", userType)
                .compact();
    }

    /**
     * Generates a JWT token with the specified username, user type, and expiration time.
     *
     * @param userType   the type of user to include as a claim in the token
     * @param username   the username to set as the subject of the token
     * @param expireTime the expiration time in milliseconds from the current time
     * @return the generated JWT token as a compact string
     */
    public String generateToken(Long userType, String username, Long expireTime) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .claim("usertype", userType)
                .compact();
    }

    /**
     * Generates an access token for the authenticated user with the specified user type.
     *
     * @param authentication the {@link Authentication} object containing user details
     * @param userType       the type of user to include as a claim in the token
     * @return the generated access token as a compact string
     */
    public String generateAccessToken(Authentication authentication, Long userType) {
        return generateToken(authentication, userType, accessTokenExpire);
    }

    /**
     * Generates a refresh token for the authenticated user with the specified user type.
     *
     * @param authentication the {@link Authentication} object containing user details
     * @param userType       the type of user to include as a claim in the token
     * @return the generated refresh token as a compact string
     */
    public String generateRefreshToken(Authentication authentication, Long userType) {
        return generateToken(authentication, userType, refreshTokenExpire);
    }

    /**
     * Generates an access token for the specified username and user type.
     *
     * @param userType the type of user to include as a claim in the token
     * @param userName the username to set as the subject of the token
     * @return the generated access token as a compact string
     */
    public String generateAccessToken(Long userType, String userName) {
        return generateToken(userType, userName, accessTokenExpire);
    }

    /**
     * Generates a refresh token for the specified username and user type.
     *
     * @param userType the type of user to include as a claim in the token
     * @param userName the username to set as the subject of the token
     * @return the generated refresh token as a compact string
     */
    public String generateRefreshToken(Long userType, String userName) {
        return generateToken(userType, userName, refreshTokenExpire);
    }

    /**
     * Returns the configured expiration time for access tokens.
     *
     * @return the expiration time in milliseconds for access tokens
     */
    public Long accessTokenExpire() {
        return accessTokenExpire;
    }

    /**
     * Extracts the username (subject) from the provided JWT token.
     *
     * @param token the JWT token to parse
     * @return the username extracted from the token's subject claim
     */
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Extracts the user type from the provided JWT token.
     *
     * @param token the JWT token to parse
     * @return the user type extracted from the token's "usertype" claim as a string
     */
    public String getUserTypeFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        return claims.get("usertype").toString();
    }

    /**
     * Validates the provided JWT token by checking its signature and expiration.
     *
     * @param token the JWT token to validate
     * @return {@code true} if the token is valid, otherwise throws an exception
     * @throws AuthenticationCredentialsNotFoundException if the token is invalid or cannot be parsed
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            throw new AuthenticationCredentialsNotFoundException("JWT token is not valid " + token);
        }
    }
}