package com.nazran.chat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket error DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketErrorDto {

    private String errorType;
    private String message;
    private Integer conversationId;
    private Integer messageId;
    private String timestamp;
}