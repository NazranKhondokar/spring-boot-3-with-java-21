package com.nazran.chat.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket DTO for read receipt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptDto {

    private Integer conversationId;
    private Integer messageId;
    private Integer userId;
    private String timestamp;
}