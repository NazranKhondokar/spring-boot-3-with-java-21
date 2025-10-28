package com.nazran.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {
    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotBlank(message = "Initial message is required")
    private String initialMessage;
}
