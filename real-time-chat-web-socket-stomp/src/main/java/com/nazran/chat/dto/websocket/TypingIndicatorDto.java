package com.nazran.chat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket DTO for typing indicator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorDto {

    private Integer conversationId;
    private Integer userId;
    private String userName;
    private Boolean isTyping;
}