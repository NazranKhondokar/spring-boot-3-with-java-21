package com.nazran.chat.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Response DTO representing a Role entity.
 * <p>
 * This class is used to transfer role data from the backend to the client
 * in a safe and structured manner.
 * </p>
 */
@Getter
@Setter
@Builder
public class RoleResponse {

    /**
     * The unique identifier of the role.
     */
    private Integer id;

    /**
     * The name of the role.
     */
    private String name;

    /**
     * The timestamp when the role was created.
     */
    private OffsetDateTime createdAt;
}
