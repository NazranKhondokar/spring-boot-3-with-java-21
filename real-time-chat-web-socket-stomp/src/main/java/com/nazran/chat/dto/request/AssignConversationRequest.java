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
public class AssignConversationRequest {
    @NotNull(message = "Conversation ID is required")
    private Integer conversationId;

    @NotNull(message = "Super admin ID is required")
    private Integer superAdminId;
}