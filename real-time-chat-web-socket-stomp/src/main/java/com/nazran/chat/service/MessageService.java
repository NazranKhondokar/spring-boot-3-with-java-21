package com.nazran.chat.service;

import com.nazran.chat.dto.response.MessageAttachmentResponse;
import com.nazran.chat.dto.response.MessageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for message-related operations.
 */
public interface MessageService {

    /**
     * Upload file attachment for a message.
     *
     * @param conversationId the conversation ID
     * @param file the file to upload
     * @param authenticatedUserId the ID of the authenticated user
     * @return message with attachment
     */
    MessageResponse sendMessageWithAttachment(Integer conversationId, MultipartFile file, String caption, Integer authenticatedUserId);

    /**
     * Get all attachments in a conversation.
     *
     * @param conversationId the conversation ID
     * @param authenticatedUserId the ID of the authenticated user
     * @return list of attachments
     */
    List<MessageAttachmentResponse> getConversationAttachments(Integer conversationId, Integer authenticatedUserId);

    /**
     * Delete a message (soft delete or mark as deleted).
     *
     * @param messageId the message ID
     * @param authenticatedUserId the ID of the authenticated user
     */
    void deleteMessage(Integer messageId, Integer authenticatedUserId);
}
