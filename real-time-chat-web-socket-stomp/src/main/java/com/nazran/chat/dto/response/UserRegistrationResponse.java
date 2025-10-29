package com.nazran.chat.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for user registration response.
 */
@Data
@Builder
public class UserRegistrationResponse {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean isEmailVerified;
    private String firebaseUserId;
}
