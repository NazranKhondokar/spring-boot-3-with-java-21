package com.nazran.chat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Generic WebSocket event DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketEventDto {

    private String eventType;
    private Integer conversationId;
    private Integer userId;
    private String userName;
    private String timestamp;
    private Map<String, Object> data;
}