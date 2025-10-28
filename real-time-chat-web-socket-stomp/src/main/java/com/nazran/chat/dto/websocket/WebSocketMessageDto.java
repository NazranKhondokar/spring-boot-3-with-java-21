package com.nazran.chat.dto.websocket;

import com.nazran.chat.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * WebSocket DTO for real-time message delivery.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDto {

    private String eventType; // MESSAGE_SENT, MESSAGE_READ, TYPING, USER_ONLINE, USER_OFFLINE
    private Integer conversationId;
    private Integer messageId;
    private Integer senderId;
    private String senderName;
    private String content;
    private MessageType messageType;
    private Boolean isRead;
    private OffsetDateTime timestamp;
}