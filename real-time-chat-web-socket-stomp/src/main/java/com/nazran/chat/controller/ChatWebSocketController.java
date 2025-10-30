package com.nazran.chat.controller;

import com.nazran.chat.dto.request.MarkAsReadRequest;
import com.nazran.chat.dto.request.SendMessageRequest;
import com.nazran.chat.dto.request.TypingIndicatorRequest;
import com.nazran.chat.dto.response.MessageResponse;
import com.nazran.chat.dto.websocket.ReadReceiptDto;
import com.nazran.chat.dto.websocket.TypingIndicatorDto;
import com.nazran.chat.entity.User;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.UserRepository;
import com.nazran.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket controller for handling real-time chat messages.
 * Uses STOMP protocol over WebSocket.
 * Updated to use path variables instead of @AuthenticationPrincipal
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Chat WebSocket", description = "Real-time chat messaging API over WebSocket using STOMP protocol")
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * Handle incoming chat messages from clients.
     * Message is sent to /app/chat/send/{firebaseUserId} and broadcasted to conversation subscribers.
     *
     * URL: /app/chat/send/{firebaseUserId}
     *
     * @param firebaseUserId the Firebase user ID from path
     * @param request        the message request
     */
    @MessageMapping("/chat/send/{firebaseUserId}")
    @Operation(
            summary = "Send a chat message",
            description = "Sends a message to a conversation and broadcasts it to all participants subscribed to the conversation topic."
    )
    public void handleSendMessage(
            @DestinationVariable String firebaseUserId,
            @Parameter(description = "Message request containing conversation ID, content, and optional metadata")
            @Payload SendMessageRequest request) {

        log.info("Received message from user: {} for conversation: {}",
                firebaseUserId, request.getConversationId());

        try {
            // Get user ID from Firebase UID
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
                    firebaseUserId,
                    "/queue/errors",
                    "Failed to send message: " + e.getMessage()
            );
        }
    }

    /**
     * Handle typing indicator from clients.
     * Broadcasts typing status to conversation participants.
     *
     * URL: /app/chat/typing/{firebaseUserId}
     *
     * @param firebaseUserId the Firebase user ID from path
     * @param request        the typing indicator request
     */
    @MessageMapping("/chat/typing/{firebaseUserId}")
    @Operation(
            summary = "Send typing indicator",
            description = "Broadcasts typing status to other participants in the conversation. Should be sent when user starts/stops typing."
    )
    public void handleTypingIndicator(
            @DestinationVariable String firebaseUserId,
            @Parameter(description = "Typing indicator request with conversation ID and typing status")
            @Payload TypingIndicatorRequest request) {

        log.debug("Typing indicator from user: {} in conversation: {}",
                firebaseUserId, request.getConversationId());

        try {
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
     * URL: /app/chat/read/{firebaseUserId}
     *
     * @param firebaseUserId the Firebase user ID from path
     * @param request        the read receipt request
     */
    @MessageMapping("/chat/read/{firebaseUserId}")
    @Operation(
            summary = "Mark messages as read",
            description = "Marks one or more messages as read and broadcasts read receipt to conversation participants."
    )
    public void handleReadReceipt(
            @DestinationVariable String firebaseUserId,
            @Parameter(description = "Read receipt request with conversation ID and message ID(s)")
            @Payload MarkAsReadRequest request) {

        log.info("Read receipt from user: {} for conversation: {}",
                firebaseUserId, request.getConversationId());

        try {
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
     * URL: /app/chat/join/{firebaseUserId}
     *
     * @param firebaseUserId the Firebase user ID from path
     * @param conversationId the conversation ID
     */
    @MessageMapping("/chat/join/{firebaseUserId}")
    @Operation(
            summary = "Join a conversation",
            description = "Notifies other participants when a user joins a conversation. Should be called when user opens/enters a conversation."
    )
    public void handleJoinConversation(
            @DestinationVariable String firebaseUserId,
            @Parameter(description = "The ID of the conversation to join")
            @Payload Integer conversationId) {

        log.info("User {} joining conversation: {}", firebaseUserId, conversationId);

        try {
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
     * URL: /app/chat/leave/{firebaseUserId}
     *
     * @param firebaseUserId the Firebase user ID from path
     * @param conversationId the conversation ID
     */
    @MessageMapping("/chat/leave/{firebaseUserId}")
    @Operation(
            summary = "Leave a conversation",
            description = "Notifies other participants when a user leaves a conversation. Should be called when user closes/exits a conversation."
    )
    public void handleLeaveConversation(
            @DestinationVariable String firebaseUserId,
            @Parameter(description = "The ID of the conversation to leave")
            @Payload Integer conversationId) {

        log.info("User {} leaving conversation: {}", firebaseUserId, conversationId);

        try {
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
     * Retrieves user by Firebase User ID or throws an exception.
     *
     * @param firebaseUserId the Firebase user ID to search for
     * @return the user ID
     * @throws CustomMessagePresentException if no user found with the Firebase ID
     */
    private Integer getUserIdFromFirebaseUid(String firebaseUserId) {
        User user = userRepository.findByFirebaseUserId(firebaseUserId)
                .orElseThrow(() -> new CustomMessagePresentException("User does not exist for this Firebase User Id"));
        return user.getId();
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