package com.nazran.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * DTO for login response.
 */
@Data
@Builder
public class LoginResponse {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean isEmailVerified;
    private String firebaseUserId;
    private String idToken;
    private Boolean isProfileComplete;
    private Integer specialistId;
    private Integer specialistCategoryId;
    private Set<RoleResponse> roles;
}
