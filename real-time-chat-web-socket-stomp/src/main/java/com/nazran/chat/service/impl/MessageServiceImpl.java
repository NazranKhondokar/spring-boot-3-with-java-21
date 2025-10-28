package com.nazran.chat.service.impl;

import com.nazran.chat.dto.response.MessageAttachmentResponse;
import com.nazran.chat.dto.response.MessageResponse;
import com.nazran.chat.entity.Conversation;
import com.nazran.chat.entity.MediaStorage;
import com.nazran.chat.entity.Message;
import com.nazran.chat.entity.User;
import com.nazran.chat.enums.MessageType;
import com.nazran.chat.enums.ReferenceType;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.ConversationRepository;
import com.nazran.chat.repository.MediaStorageRepository;
import com.nazran.chat.repository.MessageRepository;
import com.nazran.chat.repository.UserRepository;
import com.nazran.chat.service.FirebaseStorageService;
import com.nazran.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of MessageService.
 * Handles message attachments using MediaStorage and FirebaseStorageService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MediaStorageRepository mediaStorageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository chatUserRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${chat.file.max-size:10485760}") // 10MB default
    private Long maxFileSize;

    @Value("${chat.file.allowed-formats:image/jpeg,image/png,image/jpg,application/pdf,image/gif,video/mp4,audio/mpeg}")
    private String allowedFormats;

    @Override
    @Transactional
    public MessageResponse sendMessageWithAttachment(
            Integer conversationId,
            MultipartFile file,
            String caption,
            Integer authenticatedUserId) {

        log.info("Sending message with attachment to conversation ID: {}", conversationId);

        // Validate file
        validateFile(file);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new CustomMessagePresentException("Conversation not found"));

        validateConversationAccess(conversation, authenticatedUserId);

        User sender = chatUserRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new CustomMessagePresentException("Sender not found"));

        // Determine message type based on file type
        MessageType messageType = determineMessageType(file.getContentType());

        // Create message first
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(caption != null && !caption.isEmpty() ? caption : file.getOriginalFilename());
        message.setMessageType(messageType);
        message.setIsRead(false);
        message = messageRepository.save(message);

        try {
            // Upload file to Firebase Storage using existing service
            firebaseStorageService.uploadFile(
                    file,
                    authenticatedUserId,
                    ReferenceType.CHAT_MESSAGE,
                    message.getId()
            );

            // Update conversation last message timestamp
            conversation.setLastMessageAt(message.getCreatedAt());
            conversationRepository.save(conversation);

            log.info("Message with attachment sent successfully with ID: {}", message.getId());

            // Broadcast message via WebSocket
            MessageResponse response = mapToMessageResponse(message);
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversationId,
                    response
            );

            return response;

        } catch (IOException e) {
            log.error("Failed to upload attachment: {}", e.getMessage(), e);
            // Clean up the message if file upload fails
            messageRepository.delete(message);
            throw new CustomMessagePresentException("Failed to upload attachment: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageAttachmentResponse> getConversationAttachments(
            Integer conversationId,
            Integer authenticatedUserId) {

        log.info("Fetching attachments for conversation ID: {}", conversationId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new CustomMessagePresentException("Conversation not found"));

        validateConversationAccess(conversation, authenticatedUserId);

        // Get all messages in the conversation
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);

        // Get all media storage entries for these messages
        return messages.stream()
                .map(Message::getId)
                .map(messageId -> mediaStorageRepository.findByReferenceIdAndReferenceType(
                        messageId, ReferenceType.CHAT_MESSAGE))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .map(this::mapToAttachmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMessage(Integer messageId, Integer authenticatedUserId) {
        log.info("Deleting message ID: {}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomMessagePresentException("Message not found"));

        // Validate user is the sender
        if (!message.getSender().getId().equals(authenticatedUserId)) {
            throw new CustomMessagePresentException("You can only delete your own messages");
        }

        // Delete associated attachment from Firebase Storage and database
        try {
            firebaseStorageService.deleteFilesByReference(
                    messageId,
                    ReferenceType.CHAT_MESSAGE
            );
            log.info("Deleted attachment for message ID: {}", messageId);
        } catch (Exception e) {
            log.error("Error deleting attachment: {}", e.getMessage(), e);
            // Continue with soft delete even if attachment deletion fails
        }

        // Soft delete: Update content to indicate deletion
        message.setContent("[Message deleted]");
        message.setMessageType(MessageType.SYSTEM);
        messageRepository.save(message);

        log.info("Message deleted successfully");

        // Broadcast deletion via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getId() + "/delete",
                messageId
        );
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomMessagePresentException("File is required");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new CustomMessagePresentException(
                    String.format("File size exceeds maximum allowed size of %dMB",
                            maxFileSize / 1024 / 1024)
            );
        }

        // Check file format
        String contentType = file.getContentType();
        if (contentType == null || !allowedFormats.contains(contentType)) {
            throw new CustomMessagePresentException(
                    "File format not allowed. Allowed formats: " + allowedFormats
            );
        }
    }

    private MessageType determineMessageType(String contentType) {
        if (contentType == null) {
            return MessageType.FILE;
        }

        if (contentType.startsWith("image/")) {
            return MessageType.IMAGE;
        }

        if (contentType.startsWith("video/")) {
            return MessageType.VIDEO;
        }

        if (contentType.startsWith("audio/")) {
            return MessageType.AUDIO;
        }

        return MessageType.FILE;
    }

    private void validateConversationAccess(Conversation conversation, Integer userId) {
        boolean hasAccess = conversation.getCustomer().getId().equals(userId) ||
                (conversation.getSuperAdmin() != null &&
                        conversation.getSuperAdmin().getId().equals(userId));

        if (!hasAccess) {
            throw new CustomMessagePresentException("You don't have access to this conversation");
        }
    }

    private MessageResponse mapToMessageResponse(Message message) {
        User sender = message.getSender();
        String senderRole = getSenderRole(message.getConversation(), sender);

        // Get attachment if exists
        List<MessageAttachmentResponse> attachments = mediaStorageRepository
                .findByReferenceIdAndReferenceType(message.getId(), ReferenceType.CHAT_MESSAGE)
                .map(this::mapToAttachmentResponse)
                .map(List::of)
                .orElse(List.of());

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(sender.getId())
                .senderName(sender.getFirstName() + " " + sender.getLastName())
                .senderRole(senderRole)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .attachments(attachments)
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    private MessageAttachmentResponse mapToAttachmentResponse(MediaStorage mediaStorage) {
        return MessageAttachmentResponse.builder()
                .id(mediaStorage.getId())
                .messageId(mediaStorage.getReferenceId())
                .fileUrl(mediaStorage.getUrl())
                .fileName(extractFileName(mediaStorage.getExternalId()))
                .fileType(mediaStorage.getMimeType())
                .fileSize(null) // File size is not stored in MediaStorage
                .createdAt(mediaStorage.getCreatedAt())
                .build();
    }

    private String extractFileName(String externalId) {
        // Extract filename from path like "CHAT_MESSAGE/123/uuid.jpg"
        if (externalId == null) {
            return "unknown";
        }
        String[] parts = externalId.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }

    private String getSenderRole(Conversation conversation, User sender) {
        if (conversation.getCustomer().getId().equals(sender.getId())) {
            return "CUSTOMER";
        } else if (conversation.getSuperAdmin() != null &&
                conversation.getSuperAdmin().getId().equals(sender.getId())) {
            return "SUPER_ADMIN";
        }
        return "SYSTEM";
    }
}