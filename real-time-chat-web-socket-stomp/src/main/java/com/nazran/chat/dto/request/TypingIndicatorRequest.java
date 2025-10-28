package com.nazran.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for typing indicator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorRequest {

    @NotNull(message = "Conversation ID is required")
    private Integer conversationId;

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Typing status is required")
    private Boolean isTyping;
}
