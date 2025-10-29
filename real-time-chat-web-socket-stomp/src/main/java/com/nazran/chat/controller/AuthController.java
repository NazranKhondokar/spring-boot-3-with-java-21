package com.nazran.chat.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.nazran.chat.dto.request.LoginRequest;
import com.nazran.chat.dto.request.UserRegistrationRequest;
import com.nazran.chat.dto.response.LoginResponse;
import com.nazran.chat.dto.response.UserRegistrationResponse;
import com.nazran.chat.entity.User;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.service.AuthService;
import com.nazran.chat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.nazran.chat.utils.ResponseBuilder.error;
import static com.nazran.chat.utils.ResponseBuilder.success;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller for handling user authentication operations such as registration, login, and email verification.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "API for user operations")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserService userService;

    /**
     * Registers a new user and sends an email verification link.
     *
     * @param request the user registration request containing user details
     * @return ResponseEntity containing a JSON object with the registration response and success message
     * @throws CustomMessagePresentException if the email is already registered
     */
    @Operation(summary = "Register a new user", description = "Registers a user and sends an email verification link.")
    @PostMapping("/register")
    public ResponseEntity<JSONObject> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        Optional<User> users = userService.findByEmailExist(request.getEmail());
        if (users.isPresent()) throw new CustomMessagePresentException("This email already registered.");
        UserRegistrationResponse response = authService.registerUser(request);
        return ok(success(response, "User registered successfully. Email verification sent.").getJson());
    }

    /**
     * Resends a verification email to an existing user.
     *
     * @param email the email address to resend the verification link to
     * @return ResponseEntity containing a JSON object with the verification link or an error message
     */
    @Operation(summary = "Resend email verification link", description = "Resends a verification email to an existing user.")
    @PostMapping("/resend-verification")
    public ResponseEntity<JSONObject> resendVerificationEmail(@RequestParam String email) {
        try {
            String verificationLink = authService.resendVerificationEmail(email);
            logger.info("Verification email link successfully generated for: {}", email);
            return ok(success(verificationLink).getJson());
        } catch (CustomMessagePresentException ex) {
            logger.error("Error while resending verification email for {}: {}", email, ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(error(null, ex.getMessage()).getJson());
        }
    }

    /**
     * Authenticates a user using a social login ID token.
     *
     * @param request the login request containing the ID token
     * @return ResponseEntity containing a JSON object with the login response and success message
     */
    @Operation(summary = "Authenticate user", description = "Handles user login")
    @PostMapping("/login")
    public ResponseEntity<JSONObject> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            LoginResponse loginResponse = authService.login(request.getIdToken());
            logger.info("Social login successfully processed for ID token.");

            authService.setIdTokenInCookie(loginResponse.getIdToken(), response, false);
            logger.info("ID token set as a secure HTTP-only cookie");

            return ok(success(loginResponse, "User successfully authenticated and saved.").getJson());
        } catch (CustomMessagePresentException ex) {
            logger.error("Error during social login: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(error(null, ex.getMessage()).getJson());
        }
    }

    /**
     * Verifies if a Firebase ID token is valid.
     *
     * @param request the login request containing the Firebase ID token
     * @return a response entity containing a JSON object with the verification result (true if valid, otherwise)
     */
    @Operation(summary = "Verify Firebase ID token", description = "Checks if the provided Firebase ID token is valid.")
    @PostMapping("/verify-token")
    public ResponseEntity<JSONObject> verifyToken(@Valid @RequestBody LoginRequest request) {
        try {
            authService.verifyToken(request.getIdToken());
            logger.info("Token verification successful for ID token");
            return ok(success(null, "Token is valid.").getJson());
        } catch (FirebaseAuthException e) {
            logger.warn("Token verification failed for ID token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(error("Invalid or expired token.").getJson());
        }
    }


    /**
     * Refreshes a Firebase ID token and sets it as a secure HTTP-only cookie.
     *
     * @param request  the login request containing the Firebase ID token
     * @param response the HTTP response to set the cookie
     * @return a response entity containing a JSON object with the refresh result
     */
    @Operation(summary = "Refresh Firebase ID token", description = "Verifies and refreshes the Firebase ID token, setting it as a secure HTTP-only cookie.")
    @PostMapping("/refresh-token")
    public ResponseEntity<JSONObject> refreshToken(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            authService.verifyToken(request.getIdToken());
            logger.info("Token verification successful for ID token.");

            authService.setIdTokenInCookie(request.getIdToken(), response, false);
            logger.info("ID token set as a secure HTTP-only cookie.");

            return ResponseEntity.ok(success(null, "Token refreshed and set successfully.").getJson());

        } catch (FirebaseAuthException e) {
            logger.warn("Token verification failed for ID token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(error("Invalid or expired token.").getJson());
        }
    }

    /**
     * Logs out the user by clearing the idToken cookie.
     *
     * @param response the HTTP response to clear the cookie
     * @return ResponseEntity containing a JSON object with the logout success message
     */
    @Operation(summary = "Logout user", description = "Clears the idToken cookie to log out the user.")
    @PostMapping("/logout")
    public ResponseEntity<JSONObject> logout(HttpServletResponse response) {
        authService.setIdTokenInCookie(null, response, true);
        logger.info("User logged out successfully, idToken cookie cleared.");

        return ok(success(null, "User logged out successfully.").getJson());
    }
}