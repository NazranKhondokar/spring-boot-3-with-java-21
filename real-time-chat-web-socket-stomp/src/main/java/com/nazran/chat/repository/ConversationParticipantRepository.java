package com.nazran.chat.repository;

import com.nazran.chat.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ConversationParticipant entity operations.
 */
@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Integer> {

    /**
     * Find all active participants in a conversation.
     *
     * @param conversationId the conversation ID
     * @return list of active participants
     */
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId " +
            "AND cp.isActive = true ORDER BY cp.joinedAt ASC")
    List<ConversationParticipant> findActiveByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * Find all participants (active and inactive) in a conversation.
     *
     * @param conversationId the conversation ID
     * @return list of all participants
     */
    List<ConversationParticipant> findByConversationId(Integer conversationId);

    /**
     * Find all conversations a user is participating in.
     *
     * @param userId the user ID
     * @return list of participant records
     */
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.user.id = :userId AND cp.isActive = true " +
            "ORDER BY cp.joinedAt DESC")
    List<ConversationParticipant> findActiveByUserId(@Param("userId") Integer userId);

    /**
     * Check if a user is an active participant in a conversation.
     *
     * @param conversationId the conversation ID
     * @param userId         the user ID
     * @return true if user is an active participant
     */
    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END FROM ConversationParticipant cp " +
            "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId AND cp.isActive = true")
    boolean isActiveParticipant(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    /**
     * Find participant record for a user in a conversation.
     *
     * @param conversationId the conversation ID
     * @param userId         the user ID
     * @return Optional containing participant record if found
     */
    Optional<ConversationParticipant> findByConversationIdAndUserId(Integer conversationId, Integer userId);

    /**
     * Count active participants in a conversation.
     *
     * @param conversationId the conversation ID
     * @return participant count
     */
    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId " +
            "AND cp.isActive = true")
    Long countActiveByConversationId(@Param("conversationId") Integer conversationId);
}
