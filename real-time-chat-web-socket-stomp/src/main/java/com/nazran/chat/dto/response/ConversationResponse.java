package com.nazran.chat.dto.response;

import com.nazran.chat.enums.ConversationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private Integer id;
    private ChatUserResponse customer;
    private ChatUserResponse superAdmin;
    private ConversationStatus status;
    private MessageResponse lastMessage;
    private Long unreadCount;
    private OffsetDateTime lastMessageAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

