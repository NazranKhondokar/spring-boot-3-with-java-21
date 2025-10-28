package com.nazran.chat.dto.response;

import com.nazran.chat.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Integer id;
    private Integer conversationId;
    private Integer senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private MessageType messageType;
    private Boolean isRead;
    private OffsetDateTime readAt;
    private List<MessageAttachmentResponse> attachments;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
