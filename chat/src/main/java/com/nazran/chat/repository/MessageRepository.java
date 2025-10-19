package com.nazran.chat.repository;

import com.nazran.chat.entity.Message;
import com.nazran.chat.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository interface for Message entity operations.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    /**
     * Find messages by conversation ID, ordered by creation time descending (latest first).
     *
     * @param conversationId the conversation ID
     * @param pageable pagination parameters
     * @return page of messages
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    Page<Message> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Integer conversationId,
                                                           Pageable pageable);

    /**
     * Find messages by conversation ID, ordered by creation time ascending (oldest first).
     *
     * @param conversationId the conversation ID
     * @param pageable pagination parameters
     * @return page of messages
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC")
    Page<Message> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") Integer conversationId,
                                                          Pageable pageable);

    /**
     * Count unread messages in a conversation for a specific user (recipient).
     *
     * @param conversationId the conversation ID
     * @param senderId the sender ID to exclude
     * @return count of unread messages
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.isRead = false AND m.sender.id != :senderId")
    Long countUnreadByConversationIdAndNotSender(@Param("conversationId") Integer conversationId,
                                                 @Param("senderId") Integer senderId);

    /**
     * Find unread messages in a conversation for a specific user.
     *
     * @param conversationId the conversation ID
     * @param senderId the sender ID to exclude
     * @return list of unread messages
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.isRead = false AND m.sender.id != :senderId ORDER BY m.createdAt ASC")
    List<Message> findUnreadByConversationIdAndNotSender(@Param("conversationId") Integer conversationId,
                                                         @Param("senderId") Integer senderId);

    /**
     * Mark all messages in a conversation as read for a specific recipient.
     *
     * @param conversationId the conversation ID
     * @param senderId the sender ID to exclude (messages sent by this user won't be marked)
     * @param readAt the read timestamp
     * @return number of messages updated
     */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = :readAt " +
            "WHERE m.conversation.id = :conversationId AND m.sender.id != :senderId AND m.isRead = false")
    int markAllAsReadInConversation(@Param("conversationId") Integer conversationId,
                                    @Param("senderId") Integer senderId,
                                    @Param("readAt") OffsetDateTime readAt);

    /**
     * Find the last message in a conversation.
     *
     * @param conversationId the conversation ID
     * @return the last message or null if no messages exist
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findLastMessageByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * Find messages by type in a conversation.
     *
     * @param conversationId the conversation ID
     * @param messageType the message type
     * @param pageable pagination parameters
     * @return page of messages
     */
    Page<Message> findByConversationIdAndMessageType(Integer conversationId,
                                                     MessageType messageType,
                                                     Pageable pageable);

    /**
     * Count total messages in a conversation.
     *
     * @param conversationId the conversation ID
     * @return message count
     */
    Long countByConversationId(Integer conversationId);

    /**
     * Find messages sent by a user in a conversation.
     *
     * @param conversationId the conversation ID
     * @param senderId the sender ID
     * @param pageable pagination parameters
     * @return page of messages
     */
    Page<Message> findByConversationIdAndSenderId(Integer conversationId,
                                                  Integer senderId,
                                                  Pageable pageable);
}
