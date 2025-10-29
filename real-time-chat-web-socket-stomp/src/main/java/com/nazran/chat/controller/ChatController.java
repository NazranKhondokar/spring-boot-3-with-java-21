package com.nazran.chat.controller;

import com.nazran.chat.dto.request.AssignConversationRequest;
import com.nazran.chat.dto.request.CreateConversationRequest;
import com.nazran.chat.dto.request.MarkAsReadRequest;
import com.nazran.chat.dto.request.SendMessageRequest;
import com.nazran.chat.dto.response.ChatStatsResponse;
import com.nazran.chat.dto.response.ConversationResponse;
import com.nazran.chat.dto.response.MessageResponse;
import com.nazran.chat.dto.response.UnreadCountResponse;
import com.nazran.chat.entity.User;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.UserRepository;
import com.nazran.chat.service.ChatService;
import com.nazran.chat.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.nazran.chat.utils.ResponseBuilder.success;
import static org.springframework.http.ResponseEntity.ok;

/**
 * REST controller for chat operations.
 * Provides HTTP endpoints for conversation and message management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat conversation and messaging APIs")
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;
    private final UserRepository userRepository;

    /**
     * Create a new conversation with initial message.
     *
     * @param request        the conversation creation request
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return created conversation
     */
    @Operation(summary = "Create conversation", description = "Customer creates a new chat conversation")
    @PostMapping("/conversations")
    public ResponseEntity<JSONObject> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Creating conversation for user: {}", firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        ConversationResponse response = chatService.createConversation(request, userId);

        return ok(success(response, "Conversation created successfully").getJson());
    }

    /**
     * Get all conversations for authenticated user.
     *
     * @param page           page number (default 0)
     * @param size           page size (default 20)
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return paginated conversations
     */
    @Operation(summary = "Get conversations", description = "Get all conversations for current user")
    @GetMapping("/conversations")
    public ResponseEntity<JSONObject> getUserConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Fetching conversations for user: {}", firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        Pageable pageable = PageRequest.of(page, size);
        Page<ConversationResponse> conversations = chatService.getUserConversations(userId, pageable);

        return ok(success(conversations, "Conversations fetched successfully").getJson());
    }

    /**
     * Get a specific conversation by ID.
     *
     * @param conversationId the conversation ID
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return conversation details
     */
    @Operation(summary = "Get conversation", description = "Get specific conversation by ID")
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<JSONObject> getConversation(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Fetching conversation {} for user: {}", conversationId, firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        ConversationResponse response = chatService.getConversationById(conversationId, userId);

        return ok(success(response, "Conversation fetched successfully").getJson());
    }

    /**
     * Get unassigned conversations (Super Admin only).
     *
     * @param page page number
     * @param size page size
     * @return paginated unassigned conversations
     */
    @Operation(summary = "Get unassigned conversations", description = "Get conversations not yet assigned to admin")
    @GetMapping("/conversations/unassigned")
    public ResponseEntity<JSONObject> getUnassignedConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching unassigned conversations");

        Pageable pageable = PageRequest.of(page, size);
        Page<ConversationResponse> conversations = chatService.getUnassignedConversations(pageable);

        return ok(success(conversations, "Unassigned conversations fetched successfully").getJson());
    }

    /**
     * Assign conversation to a super admin.
     *
     * @param request        the assignment request
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return updated conversation
     */
    @Operation(summary = "Assign conversation", description = "Assign conversation to a super admin")
    @PutMapping("/conversations/assign")
    public ResponseEntity<JSONObject> assignConversation(
            @Valid @RequestBody AssignConversationRequest request,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Assigning conversation {} to admin {}", request.getConversationId(), request.getSuperAdminId());

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        ConversationResponse response = chatService.assignConversation(request, userId);

        return ok(success(response, "Conversation assigned successfully").getJson());
    }

    /**
     * Close a conversation.
     *
     * @param conversationId the conversation ID
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return updated conversation
     */
    @Operation(summary = "Close conversation", description = "Close a conversation")
    @PutMapping("/conversations/{conversationId}/close")
    public ResponseEntity<JSONObject> closeConversation(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Closing conversation {} by user: {}", conversationId, firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        ConversationResponse response = chatService.closeConversation(conversationId, userId);

        return ok(success(response, "Conversation closed successfully").getJson());
    }

    /**
     * Send a message in a conversation.
     *
     * @param request        the message request
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return sent message
     */
    @Operation(summary = "Send message", description = "Send a message in a conversation")
    @PostMapping("/messages")
    public ResponseEntity<JSONObject> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Sending message to conversation {} by user: {}", request.getConversationId(), firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        MessageResponse response = chatService.sendMessage(request, userId);

        return ok(success(response, "Message sent successfully").getJson());
    }

    /**
     * Get messages in a conversation.
     *
     * @param conversationId the conversation ID
     * @param page           page number
     * @param size           page size
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return paginated messages
     */
    @Operation(summary = "Get messages", description = "Get messages in a conversation")
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<JSONObject> getConversationMessages(
            @PathVariable Integer conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Fetching messages for conversation {} by user: {}", conversationId, firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponse> messages = chatService.getConversationMessages(conversationId, userId, pageable);

        return ok(success(messages, "Messages fetched successfully").getJson());
    }

    /**
     * Mark messages as read.
     *
     * @param request        the read request
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return success response
     */
    @Operation(summary = "Mark as read", description = "Mark messages as read")
    @PutMapping("/messages/read")
    public ResponseEntity<JSONObject> markMessagesAsRead(
            @Valid @RequestBody MarkAsReadRequest request,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Marking messages as read in conversation {} by user: {}",
                request.getConversationId(), firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        chatService.markMessagesAsRead(request, userId);

        return ok(success(null, "Messages marked as read").getJson());
    }

    /**
     * Send message with file attachment.
     *
     * @param conversationId the conversation ID
     * @param file           the file to upload
     * @param caption        optional caption
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return message with attachment
     */
    @Operation(summary = "Send attachment", description = "Send message with file attachment")
    @PostMapping("/messages/attachment")
    public ResponseEntity<JSONObject> sendMessageWithAttachment(
            @RequestParam Integer conversationId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String caption,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Sending message with attachment to conversation {} by user: {}",
                conversationId, firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        MessageResponse response = messageService.sendMessageWithAttachment(conversationId, file, caption, userId);

        return ok(success(response, "Message with attachment sent successfully").getJson());
    }

    /**
     * Get total unread count for user.
     *
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return total unread count
     */
    @Operation(summary = "Get total unread", description = "Get total unread message count")
    @GetMapping("/unread/total")
    public ResponseEntity<JSONObject> getTotalUnreadCount(@AuthenticationPrincipal String firebaseUserId) {

        log.info("Fetching total unread count for user: {}", firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        Long unreadCount = chatService.getTotalUnreadCount(userId);

        return ok(success(java.util.Map.of("totalUnread", unreadCount),
                "Unread count fetched successfully").getJson());
    }

    /**
     * Get unread count for a conversation.
     *
     * @param conversationId the conversation ID
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return unread count
     */
    @Operation(summary = "Get conversation unread", description = "Get unread count for specific conversation")
    @GetMapping("/conversations/{conversationId}/unread")
    public ResponseEntity<JSONObject> getConversationUnreadCount(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Fetching unread count for conversation {} by user: {}",
                conversationId, firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        UnreadCountResponse response = chatService.getConversationUnreadCount(conversationId, userId);

        return ok(success(response, "Unread count fetched successfully").getJson());
    }

    /**
     * Get chat statistics.
     *
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return chat statistics
     */
    @Operation(summary = "Get chat stats", description = "Get chat statistics for dashboard")
    @GetMapping("/stats")
    public ResponseEntity<JSONObject> getChatStats(@AuthenticationPrincipal String firebaseUserId) {

        log.info("Fetching chat statistics for user: {}", firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        ChatStatsResponse response = chatService.getChatStats(userId);

        return ok(success(response, "Chat statistics fetched successfully").getJson());
    }

    /**
     * Search conversations.
     *
     * @param searchTerm     the search term
     * @param page           page number
     * @param size           page size
     * @param firebaseUserId the authenticated user's Firebase UID
     * @return paginated search results
     */
    @Operation(summary = "Search conversations", description = "Search conversations by content or participant name")
    @GetMapping("/conversations/search")
    public ResponseEntity<JSONObject> searchConversations(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("Searching conversations with term: {} for user: {}", searchTerm, firebaseUserId);

        Integer userId = getUserIdFromPrincipal(firebaseUserId);
        Pageable pageable = PageRequest.of(page, size);
        Page<ConversationResponse> conversations = chatService.searchConversations(userId, searchTerm, pageable);

        return ok(success(conversations, "Search completed successfully").getJson());
    }

    /**
     * Retrieves user by Firebase User ID or throws an exception.
     *
     * @param firebaseUserId the Firebase user ID to search for
     * @return the found User entity
     * @throws CustomMessagePresentException if no user found with the Firebase ID
     */
    private Integer getUserIdFromPrincipal(String firebaseUserId) {
        User user = userRepository.findByFirebaseUserId(firebaseUserId)
                .orElseThrow(() -> new CustomMessagePresentException("User does not exist for this Firebase User Id"));
        return user.getId();
    }
}
