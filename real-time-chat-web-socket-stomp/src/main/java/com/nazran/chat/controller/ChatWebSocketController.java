package com.nazran.chat.controller;

import com.nazran.chat.dto.request.MarkAsReadRequest;
import com.nazran.chat.dto.request.SendMessageRequest;
import com.nazran.chat.dto.request.TypingIndicatorRequest;
import com.nazran.chat.dto.response.MessageResponse;
import com.nazran.chat.dto.websocket.ReadReceiptDto;
import com.nazran.chat.dto.websocket.TypingIndicatorDto;
import com.nazran.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket controller for handling real-time chat messages.
 * Uses STOMP protocol over WebSocket.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle incoming chat messages from clients.
     * Message is sent to /app/chat/send and broadcasted to conversation subscribers.
     *
     * @param request   the message request
     * @param principal the authenticated user
     * @return message response
     */
    @MessageMapping("/chat/send")
    public void handleSendMessage(
            @Payload SendMessageRequest request,
            Principal principal) {

        log.info("Received message from user: {} for conversation: {}",
                principal.getName(), request.getConversationId());

        try {
            // Get user ID from principal (Firebase UID)
            String firebaseUserId = principal.getName();

            // TODO: Get actual user ID from Firebase UID
            // For now, assuming you have a method to get user ID
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            // Send message through service
            MessageResponse response = chatService.sendMessage(request, userId);

            // Broadcast to conversation subscribers
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + request.getConversationId(),
                    response
            );

            log.info("Message sent and broadcasted successfully");

        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);

            // Send error back to sender
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    "Failed to send message: " + e.getMessage()
            );
        }
    }

    /**
     * Handle typing indicator from clients.
     * Broadcasts typing status to conversation participants.
     *
     * @param request   the typing indicator request
     * @param principal the authenticated user
     */
    @MessageMapping("/chat/typing")
    public void handleTypingIndicator(
            @Payload TypingIndicatorRequest request,
            Principal principal) {

        log.debug("Typing indicator from user: {} in conversation: {}",
                principal.getName(), request.getConversationId());

        try {
            String firebaseUserId = principal.getName();
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            // Create typing indicator DTO
            TypingIndicatorDto typingDto = TypingIndicatorDto.builder()
                    .conversationId(request.getConversationId())
                    .userId(userId)
                    .userName(getUserName(userId))
                    .isTyping(request.getIsTyping())
                    .build();

            // Broadcast to conversation (exclude sender)
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + request.getConversationId() + "/typing",
                    typingDto
            );

        } catch (Exception e) {
            log.error("Error handling typing indicator: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle read receipt from clients.
     * Marks messages as read and broadcasts to conversation participants.
     *
     * @param request   the read receipt request
     * @param principal the authenticated user
     */
    @MessageMapping("/chat/read")
    public void handleReadReceipt(
            @Payload MarkAsReadRequest request,
            Principal principal) {

        log.info("Read receipt from user: {} for conversation: {}",
                principal.getName(), request.getConversationId());

        try {
            String firebaseUserId = principal.getName();
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            // Mark messages as read
            chatService.markMessagesAsRead(request, userId);

            // Create read receipt DTO
            ReadReceiptDto receiptDto = ReadReceiptDto.builder()
                    .conversationId(request.getConversationId())
                    .messageId(request.getMessageId())
                    .userId(userId)
                    .timestamp(OffsetDateTime.now(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .build();

            // Broadcast to conversation
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + request.getConversationId() + "/read",
                    receiptDto
            );

            log.info("Messages marked as read and receipt broadcasted");

        } catch (Exception e) {
            log.error("Error handling read receipt: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user joining a conversation.
     * Notifies other participants.
     *
     * @param conversationId the conversation ID
     * @param principal      the authenticated user
     */
    @MessageMapping("/chat/join")
    public void handleJoinConversation(
            @Payload Integer conversationId,
            Principal principal) {

        log.info("User {} joining conversation: {}", principal.getName(), conversationId);

        try {
            String firebaseUserId = principal.getName();
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            // Broadcast join event
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversationId + "/events",
                    createEvent("USER_JOINED", userId, conversationId)
            );

        } catch (Exception e) {
            log.error("Error handling join conversation: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user leaving a conversation.
     * Notifies other participants.
     *
     * @param conversationId the conversation ID
     * @param principal      the authenticated user
     */
    @MessageMapping("/chat/leave")
    public void handleLeaveConversation(
            @Payload Integer conversationId,
            Principal principal) {

        log.info("User {} leaving conversation: {}", principal.getName(), conversationId);

        try {
            String firebaseUserId = principal.getName();
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            // Broadcast leave event
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversationId + "/events",
                    createEvent("USER_LEFT", userId, conversationId)
            );

        } catch (Exception e) {
            log.error("Error handling leave conversation: {}", e.getMessage(), e);
        }
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    /**
     * TODO: Implement this method to get user ID from Firebase UID.
     * You should query your user repository to get the internal user ID.
     */
    private Integer getUserIdFromFirebaseUid(String firebaseUserId) {
        // Placeholder - implement actual logic
        // Example:
        // return userRepository.findByFirebaseUserId(firebaseUserId)
        //         .map(User::getId)
        //         .orElseThrow(() -> new CustomMessagePresentException("User not found"));

        log.warn("TODO: Implement getUserIdFromFirebaseUid - returning mock ID");
        return 1; // Replace with actual implementation
    }

    /**
     * TODO: Implement this method to get user name.
     */
    private String getUserName(Integer userId) {
        // Placeholder - implement actual logic
        log.warn("TODO: Implement getUserName - returning mock name");
        return "User " + userId; // Replace with actual implementation
    }

    /**
     * Create a generic event object.
     */
    private Object createEvent(String eventType, Integer userId, Integer conversationId) {
        return java.util.Map.of(
                "eventType", eventType,
                "userId", userId,
                "conversationId", conversationId,
                "timestamp", OffsetDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }
}