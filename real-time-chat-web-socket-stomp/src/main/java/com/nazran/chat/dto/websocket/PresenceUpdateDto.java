package com.nazran.chat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket DTO for user presence updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceUpdateDto {

    private Integer userId;
    private String userName;
    private Boolean isOnline;
    private String timestamp;
}