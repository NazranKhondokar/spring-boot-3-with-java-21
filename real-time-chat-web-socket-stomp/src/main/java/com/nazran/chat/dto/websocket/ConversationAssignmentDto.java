package com.nazran.chat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket DTO for conversation assignment notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationAssignmentDto {

    private Integer conversationId;
    private Integer superAdminId;
    private String superAdminName;
    private Integer customerId;
    private String customerName;
    private String timestamp;
}