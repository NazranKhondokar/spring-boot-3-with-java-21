package com.nazran.chat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Data Transfer Object (DTO) for the {@link com.nazran.chat.entity.User} entity.
 * Contains basic personal details and Firebase authentication information for transferring user data.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 256, message = "Email must not exceed 256 characters")
    private String email;

    @NotBlank(message = "Firebase user ID is required")
    private String firebaseUserId;

    private Integer id;

    private Integer[] roleIds;
}