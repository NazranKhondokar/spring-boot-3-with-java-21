package com.nazran.chat.service;

import com.nazran.chat.dto.UserDto;
import com.nazran.chat.dto.request.UserRegistrationRequest;
import com.nazran.chat.dto.response.UserRegistrationResponse;
import com.nazran.chat.entity.User;

import java.util.Optional;

/**
 * Service interface for managing user-related operations.
 */
public interface UserService {

    /**
     * Checks if a user exists by their email address and returns an optional user entity.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the {@link User} if found, or empty if not found
     */
    Optional<User> findByEmailExist(String email);

    /**
     * Retrieves a user by their ID and maps it to a DTO.
     *
     * @param id the ID of the user to retrieve
     * @return the {@link UserDto} containing the user's details
     */
    UserDto getUserById(Integer id);


    /**
     * Registers a new user or updates an existing one based on the provided request data.
     *
     * @param request the {@link UserRegistrationRequest} containing user registration details
     * @return a {@link UserRegistrationResponse} with the registered user's details
     */
    UserRegistrationResponse addUser(UserRegistrationRequest request);
}