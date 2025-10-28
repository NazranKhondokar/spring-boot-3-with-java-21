package com.nazran.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsReadRequest {
    @NotNull(message = "Conversation ID is required")
    private Integer conversationId;

    private Integer messageId; // Optional: mark specific message, null for all
}
