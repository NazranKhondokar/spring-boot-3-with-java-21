package com.nazran.chat.repository;

import com.nazran.chat.entity.Conversation;
import com.nazran.chat.enums.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Conversation entity operations.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    /**
     * Find all conversations for a customer, ordered by last message timestamp.
     *
     * @param customerId the customer ID
     * @param pageable   pagination parameters
     * @return page of conversations
     */
    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<Conversation> findByCustomerIdOrderByLastMessageAtDesc(@Param("customerId") Integer customerId,
                                                                Pageable pageable);

    /**
     * Find all conversations assigned to a super admin, ordered by last message timestamp.
     *
     * @param superAdminId the super admin ID
     * @param pageable     pagination parameters
     * @return page of conversations
     */
    @Query("SELECT c FROM Conversation c WHERE c.superAdmin.id = :superAdminId ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<Conversation> findBySuperAdminIdOrderByLastMessageAtDesc(@Param("superAdminId") Integer superAdminId,
                                                                  Pageable pageable);

    /**
     * Find conversations by status.
     *
     * @param status   the conversation status
     * @param pageable pagination parameters
     * @return page of conversations
     */
    Page<Conversation> findByStatus(ConversationStatus status, Pageable pageable);

    /**
     * Find open (unassigned) conversations.
     *
     * @param pageable pagination parameters
     * @return page of open conversations
     */
    @Query("SELECT c FROM Conversation c WHERE c.status = 'OPEN' AND c.superAdmin IS NULL ORDER BY c.createdAt ASC")
    Page<Conversation> findOpenUnassignedConversations(Pageable pageable);

    /**
     * Find active conversation between customer and super admin.
     *
     * @param customerId   the customer ID
     * @param superAdminId the super admin ID
     * @return Optional containing the conversation if found
     */
    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId AND c.superAdmin.id = :superAdminId " +
            "AND c.status != 'CLOSED' ORDER BY c.createdAt DESC")
    Optional<Conversation> findActiveConversationBetween(@Param("customerId") Integer customerId,
                                                         @Param("superAdminId") Integer superAdminId);

    /**
     * Find the most recent conversation for a customer.
     *
     * @param customerId the customer ID
     * @return Optional containing the conversation if found
     */
    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId ORDER BY c.lastMessageAt DESC NULLS LAST")
    Optional<Conversation> findMostRecentByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Count conversations by status.
     *
     * @param status the conversation status
     * @return count of conversations
     */
    Long countByStatus(ConversationStatus status);

    /**
     * Count unassigned open conversations.
     *
     * @return count of unassigned conversations
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.status = 'OPEN' AND c.superAdmin IS NULL")
    Long countUnassignedOpen();

    /**
     * Find all conversations for a user (as customer or admin).
     *
     * @param userId   the user ID
     * @param pageable pagination parameters
     * @return page of conversations
     */
    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :userId OR c.superAdmin.id = :userId " +
            "ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<Conversation> findAllByUserId(@Param("userId") Integer userId, Pageable pageable);
}