package com.nazran.chat.repository;

import com.nazran.chat.entity.ConversationUnreadCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ConversationUnreadCount entity operations.
 */
@Repository
public interface ConversationUnreadCountRepository extends JpaRepository<ConversationUnreadCount, Integer> {

    /**
     * Find unread count for a user in a conversation.
     *
     * @param conversationId the conversation ID
     * @param userId         the user ID
     * @return Optional containing unread count record if found
     */
    Optional<ConversationUnreadCount> findByConversationIdAndUserId(Integer conversationId, Integer userId);

    /**
     * Find all unread counts for a user.
     *
     * @param userId the user ID
     * @return list of unread count records
     */
    List<ConversationUnreadCount> findByUserId(Integer userId);

    /**
     * Find all conversations with unread messages for a user.
     *
     * @param userId the user ID
     * @return list of unread count records where count > 0
     */
    @Query("SELECT cuc FROM ConversationUnreadCount cuc WHERE cuc.user.id = :userId AND cuc.unreadCount > 0")
    List<ConversationUnreadCount> findUnreadByUserId(@Param("userId") Integer userId);

    /**
     * Get total unread count for a user across all conversations.
     *
     * @param userId the user ID
     * @return total unread count
     */
    @Query("SELECT COALESCE(SUM(cuc.unreadCount), 0) FROM ConversationUnreadCount cuc WHERE cuc.user.id = :userId")
    Long getTotalUnreadCountByUserId(@Param("userId") Integer userId);

    /**
     * Reset unread count for a user in a conversation.
     *
     * @param conversationId the conversation ID
     * @param userId         the user ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE ConversationUnreadCount cuc SET cuc.unreadCount = 0 " +
            "WHERE cuc.conversation.id = :conversationId AND cuc.user.id = :userId")
    int resetUnreadCount(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    /**
     * Increment unread count for a user in a conversation.
     *
     * @param conversationId the conversation ID
     * @param userId         the user ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE ConversationUnreadCount cuc SET cuc.unreadCount = cuc.unreadCount + 1 " +
            "WHERE cuc.conversation.id = :conversationId AND cuc.user.id = :userId")
    int incrementUnreadCount(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    /**
     * Delete unread count records for a conversation.
     *
     * @param conversationId the conversation ID
     */
    void deleteByConversationId(Integer conversationId);
}
