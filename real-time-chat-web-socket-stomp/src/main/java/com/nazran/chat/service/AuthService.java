package com.nazran.chat.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.nazran.chat.dto.request.UserRegistrationRequest;
import com.nazran.chat.dto.response.LoginResponse;
import com.nazran.chat.dto.response.UserRegistrationResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Service interface for user authentication and registration.
 */
public interface AuthService {

    /**
     * Registers a new user and sends a verification email.
     *
     * @param request User registration request data.
     * @return User registration response.
     */
    UserRegistrationResponse registerUser(UserRegistrationRequest request);

    /**
     * Sends an email verification link to an existing user.
     *
     * @param email Email of the user.
     * @return email verification link
     */
    String resendVerificationEmail(String email);

    /**
     * Authenticates a user using a Firebase ID token.
     *
     * @param idToken The Firebase ID token.
     * @return LoginResponse containing authentication details.
     */
    LoginResponse login(String idToken);

    /**
     * Verifies a Firebase ID token and returns the decoded token information.
     *
     * @param idToken The Firebase ID token to verify
     * @return Decoded Firebase token containing user information
     * @throws FirebaseAuthException If the token is invalid, expired, or verification fails
     */
    FirebaseToken verifyToken(String idToken) throws FirebaseAuthException;

    /**
     * Creates and configures an HTTP-only cookie for the idToken.
     *
     * @param idToken  The ID token to set in the cookie, or null to clear the cookie (for logout).
     * @param response The HTTP response to add the cookie to.
     * @param isLogout Flag to indicate if the cookie should be cleared (for logout).
     */
    void setIdTokenInCookie(String idToken, HttpServletResponse response, boolean isLogout);
}