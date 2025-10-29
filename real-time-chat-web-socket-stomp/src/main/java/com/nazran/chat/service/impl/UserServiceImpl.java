package com.nazran.chat.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.nazran.chat.dto.UserDto;
import com.nazran.chat.dto.request.UserRegistrationRequest;
import com.nazran.chat.dto.response.UserRegistrationResponse;
import com.nazran.chat.entity.Role;
import com.nazran.chat.entity.User;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.RoleRepository;
import com.nazran.chat.repository.UserRepository;
import com.nazran.chat.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Service implementation for user-related operations including OTP generation, validation,
 * and SMS communication. Handles the complete OTP lifecycle from generation to verification.
 * Implementation of the {@link UserService} interface for managing user-related operations.
 * Provides functionality for user retrieval, registration, and data mapping, integrated with Firebase authentication.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the found {@link User}, or empty if no user is found
     */
    @Override
    public Optional<User> findByEmailExist(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDto getUserById(Integer id) {
        logger.info("Fetching user by ID: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> {
            logger.error("User not found with ID: {}", id);
            return new CustomMessagePresentException("User not found with ID: " + id);
        });
        return mapToDto(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserRegistrationResponse addUser(UserRegistrationRequest request) {
        try {
            FirebaseToken decodedToken = verifyToken(request.getIdToken());
            User savedUser = addOrUpdateUser(decodedToken, request);

            logger.info("User successfully saved with ID: {}", savedUser.getId());

            return UserRegistrationResponse.builder()
                    .id(savedUser.getId())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .email(savedUser.getEmail())
                    .isEmailVerified(savedUser.getIsEmailVerified())
                    .firebaseUserId(savedUser.getFirebaseUserId())
                    .build();
        } catch (FirebaseAuthException e) {
            logger.error("Error occurred during user registration: {}", e.getMessage(), e);
            throw new CustomMessagePresentException(e.getMessage());
        }
    }

    /**
     * Adds a new user or updates an existing user based on Firebase token and request data.
     *
     * @param decodedToken the decoded Firebase token containing user authentication details
     * @param request      the {@link UserRegistrationRequest} containing additional user details
     * @return the saved or updated {@link User} entity
     * @throws IllegalArgumentException if the email is missing or empty in the Firebase token
     */
    private User addOrUpdateUser(FirebaseToken decodedToken, UserRegistrationRequest request) {
        String email = decodedToken.getEmail();
        boolean isEmailVerified = decodedToken.isEmailVerified();
        String firebaseUserId = decodedToken.getUid();

        if (email == null || email.isEmpty()) {
            logger.error("Email is missing in the ID token.");
            throw new IllegalArgumentException("Email is required but not provided by the provider.");
        }

        Optional<User> existingUser = userRepository.findByFirebaseUserId(firebaseUserId);

        User user = existingUser.orElseGet(User::new);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setFirebaseUserId(firebaseUserId);
        user.setEmail(email);
        user.setIsEmailVerified(isEmailVerified);

        if (request.getRoleIds() != null) {
            logger.info("Assigning roles to user with Firebase ID: {}. Roles: {}", firebaseUserId, request.getRoleIds());
            Set<Role> roles = new HashSet<>();
            for (Integer roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new CustomMessagePresentException("Role not found with ID: " + roleId));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        return userRepository.save(user);
    }

    /**
     * Verifies a Firebase ID token.
     *
     * @param idToken the Firebase ID token to verify
     * @return the decoded {@link FirebaseToken}
     * @throws FirebaseAuthException if the token verification fails
     */
    private FirebaseToken verifyToken(@NotBlank(message = "idToken is required") String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
        logger.info("Successfully verified ID token: {}", idToken);
        return decodedToken;
    }

    /**
     * Maps a {@link User} entity to a {@link UserDto}.
     *
     * @param user the {@link User} entity to map
     * @return the mapped {@link UserDto}
     */
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .firebaseUserId(user.getFirebaseUserId())
                .build();
    }

    /**
     * Retrieves user by Firebase User ID or throws an exception.
     *
     * @param firebaseUserId the Firebase user ID to search for
     * @return the found User entity
     * @throws CustomMessagePresentException if no user found with the Firebase ID
     */
    private User findByFirebaseUserIdOrThrow(String firebaseUserId) {
        return userRepository.findByFirebaseUserId(firebaseUserId)
                .orElseThrow(() -> new CustomMessagePresentException("User does not exist for this Firebase User Id"));
    }
}
