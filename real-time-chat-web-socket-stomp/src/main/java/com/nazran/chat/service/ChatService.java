package com.nazran.chat.service;

import com.nazran.chat.dto.request.AssignConversationRequest;
import com.nazran.chat.dto.request.CreateConversationRequest;
import com.nazran.chat.dto.request.MarkAsReadRequest;
import com.nazran.chat.dto.request.SendMessageRequest;
import com.nazran.chat.dto.response.ChatStatsResponse;
import com.nazran.chat.dto.response.ConversationResponse;
import com.nazran.chat.dto.response.MessageResponse;
import com.nazran.chat.dto.response.UnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for chat operations.
 * Handles conversations, messages, and user interactions.
 */
public interface ChatService {

    /**
     * Create a new conversation with initial message.
     *
     * @param request             the conversation creation request
     * @param authenticatedUserId the ID of the authenticated user
     * @return created conversation details
     */
    ConversationResponse createConversation(CreateConversationRequest request, Integer authenticatedUserId);

    /**
     * Get all conversations for a user (customer or admin).
     *
     * @param userId   the user ID
     * @param pageable pagination parameters
     * @return paginated list of conversations
     */
    Page<ConversationResponse> getUserConversations(Integer userId, Pageable pageable);

    /**
     * Get a specific conversation by ID.
     *
     * @param conversationId      the conversation ID
     * @param authenticatedUserId the ID of the authenticated user
     * @return conversation details
     */
    ConversationResponse getConversationById(Integer conversationId, Integer authenticatedUserId);

    /**
     * Assign a conversation to a super admin.
     *
     * @param request             the assignment request
     * @param authenticatedUserId the ID of the authenticated user
     * @return updated conversation details
     */
    ConversationResponse assignConversation(AssignConversationRequest request, Integer authenticatedUserId);

    /**
     * Close a conversation.
     *
     * @param conversationId      the conversation ID
     * @param authenticatedUserId the ID of the authenticated user
     * @return updated conversation details
     */
    ConversationResponse closeConversation(Integer conversationId, Integer authenticatedUserId);

    /**
     * Get unassigned open conversations (for admin dashboard).
     *
     * @param pageable pagination parameters
     * @return paginated list of unassigned conversations
     */
    Page<ConversationResponse> getUnassignedConversations(Pageable pageable);

    /**
     * Send a message in a conversation.
     *
     * @param request             the message sending request
     * @param authenticatedUserId the ID of the authenticated user
     * @return sent message details
     */
    MessageResponse sendMessage(SendMessageRequest request, Integer authenticatedUserId);

    /**
     * Get messages in a conversation with pagination.
     *
     * @param conversationId      the conversation ID
     * @param authenticatedUserId the ID of the authenticated user
     * @param pageable            pagination parameters
     * @return paginated list of messages
     */
    Page<MessageResponse> getConversationMessages(Integer conversationId, Integer authenticatedUserId, Pageable pageable);

    /**
     * Mark messages as read in a conversation.
     *
     * @param request             the mark as read request
     * @param authenticatedUserId the ID of the authenticated user
     */
    void markMessagesAsRead(MarkAsReadRequest request, Integer authenticatedUserId);

    /**
     * Get unread message count for a user across all conversations.
     *
     * @param userId the user ID
     * @return total unread count
     */
    Long getTotalUnreadCount(Integer userId);

    /**
     * Get unread count for a specific conversation.
     *
     * @param conversationId the conversation ID
     * @param userId         the user ID
     * @return unread count
     */
    UnreadCountResponse getConversationUnreadCount(Integer conversationId, Integer userId);

    /**
     * Search conversations by content or participant name.
     *
     * @param userId     the user ID
     * @param searchTerm the search term
     * @param pageable   pagination parameters
     * @return paginated list of matching conversations
     */
    Page<ConversationResponse> searchConversations(Integer userId, String searchTerm, Pageable pageable);

    /**
     * Get chat statistics for dashboard.
     *
     * @param userId the user ID (null for global stats)
     * @return chat statistics
     */
    ChatStatsResponse getChatStats(Integer userId);
}