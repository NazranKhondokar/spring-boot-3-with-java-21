package com.nazran.chat.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.nazran.chat.dto.request.UserRegistrationRequest;
import com.nazran.chat.dto.response.LoginResponse;
import com.nazran.chat.dto.response.RoleResponse;
import com.nazran.chat.dto.response.UserRegistrationResponse;
import com.nazran.chat.entity.Role;
import com.nazran.chat.entity.User;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.UserRepository;
import com.nazran.chat.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for user registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final FirebaseAuth firebaseAuth;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        try {
            FirebaseToken decodedToken = verifyToken(request.getIdToken());
            User savedUser = createOrUpdateUser(decodedToken, request);

            logger.info("User successfully saved with ID: {}", savedUser.getId());

            return UserRegistrationResponse.builder()
                    .id(savedUser.getId())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .email(savedUser.getEmail())
                    .isEmailVerified(savedUser.getIsEmailVerified())
                    .firebaseUserId(savedUser.getFirebaseUserId())
                    .build();
        } catch (Exception e) {
            logger.error("Error occurred during user registration: {}", e.getMessage(), e);
            throw new CustomMessagePresentException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resendVerificationEmail(String email) {
        logger.info("Attempting to resend verification email for: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User with email {} not found", email);
                    return new CustomMessagePresentException("User with email not found");
                });

        try {
            String verificationLink = firebaseAuth.generateEmailVerificationLink(email);
            logger.info("Generated verification link for {}: {}", email, verificationLink);
            return verificationLink;
        } catch (Exception ex) {
            logger.error("Error sending verification email for {}: {}", email, ex.getMessage(), ex);
            throw new CustomMessagePresentException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse login(String idToken) {
        try {
            logger.info("Starting social login process with ID token: {}", idToken);

            FirebaseToken decodedToken = verifyToken(idToken);
            boolean isEmailVerified = decodedToken.isEmailVerified();
            if (!isEmailVerified)
                throw new CustomMessagePresentException("Please verify your email.");

            User user = createOrUpdateUser(decodedToken, null);
            logger.info("Social login process completed successfully.");
            return buildResponse(user, idToken);
        } catch (FirebaseAuthException e) {
            logger.error("Firebase authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
        logger.info("Successfully verified ID token: {}", idToken);
        return decodedToken;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIdTokenInCookie(String idToken, HttpServletResponse response, boolean isLogout) {
        Cookie idTokenCookie = new Cookie("idToken", isLogout ? null : idToken);
        idTokenCookie.setHttpOnly(true); // Prevents JavaScript access for security
        idTokenCookie.setSecure(false); // Ensures cookie is sent over HTTPS
        idTokenCookie.setPath("/"); // Cookie is available for the entire application
        idTokenCookie.setMaxAge(isLogout ? 0 : 3600); // Expire immediately for logout, 1 hour otherwise
        idTokenCookie.setAttribute("SameSite", "Lax"); // Allows cross-origin requests
        response.addCookie(idTokenCookie); // Add cookie to the response
        logger.info(isLogout ? "idToken in cookie cleared for logout." : "idToken in cookie is set");
    }

    private User createOrUpdateUser(FirebaseToken decodedToken, UserRegistrationRequest request) {
        String email = decodedToken.getEmail();
        boolean isEmailVerified = decodedToken.isEmailVerified();
        String firebaseUserId = decodedToken.getUid();

        if (email == null || email.isEmpty()) {
            logger.error("Email is missing in the ID token.");
            throw new IllegalArgumentException("Email is required but not provided by the provider.");
        }

        String normalizedEmail = email.toLowerCase(Locale.ROOT);

        Optional<User> existingUser = userRepository.findByFirebaseUserId(firebaseUserId);

        User user = existingUser.orElseGet(User::new);
        user.setFirebaseUserId(firebaseUserId);
        user.setEmail(email);
        user.setIsEmailVerified(isEmailVerified);
        if (user.getRoles().size() > 0) {
            Map<String, Object> claims = new HashMap<>();

            // Convert Role entities to role names (strings)
            List<String> roleNames = user.getRoles().stream()
                    .map(Role::getName)  // Assuming Role has getName() method
                    .collect(Collectors.toList());

            claims.put("role", roleNames);

            try {
                FirebaseAuth.getInstance().setCustomUserClaims(firebaseUserId, claims);
                logger.info("Firebase custom claims set for user {}: {}", firebaseUserId, roleNames);
            } catch (FirebaseAuthException e) {
                logger.error("Failed to set Firebase custom claims for user: {}", firebaseUserId, e);
                throw new RuntimeException("Failed to set user roles in Firebase", e);
            }
        }

        // Split the name from decodedToken.getName()
        if (request == null && decodedToken.getName() != null && !decodedToken.getName().isEmpty()) {
            String fullName = decodedToken.getName().trim();
            String[] nameParts = fullName.split("\\s+"); // Split by whitespace
            if (nameParts.length > 1) {
                user.setFirstName(String.join(" ", Arrays.copyOf(nameParts, nameParts.length - 1))); // All except the last
                user.setLastName(nameParts[nameParts.length - 1]); // Last part as last name
            } else {
                user.setFirstName(fullName); // Only one part, set as first name
                user.setLastName(""); // Empty last name
            }
        }

        // Override with request values if available
        if (request != null) {
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
        }

        return userRepository.save(user);
    }

    private LoginResponse buildResponse(User user, String idToken) {
        Set<Role> roles = user.getRoles();
        logger.info("User roles count: {}", roles != null ? roles.size() : 0);

        return LoginResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .isEmailVerified(user.getIsEmailVerified())
                .firebaseUserId(user.getFirebaseUserId())
                .idToken(idToken)
                .roles(mapRolesToResponse(Objects.requireNonNull(roles)))
                .build();
    }

    private Set<RoleResponse> mapRolesToResponse(Set<Role> roles) {
        return roles.stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .build())
                .collect(Collectors.toSet());
    }
}