package com.nazran.chat.repository;

import com.nazran.chat.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for MessageAttachment entity operations.
 */
@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Integer> {

    /**
     * Find all attachments for a message.
     *
     * @param messageId the message ID
     * @return list of attachments
     */
    List<MessageAttachment> findByMessageId(Integer messageId);

    /**
     * Find all attachments in a conversation.
     *
     * @param conversationId the conversation ID
     * @return list of attachments
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.message.conversation.id = :conversationId " +
            "ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * Find attachments by file type in a conversation.
     *
     * @param conversationId the conversation ID
     * @param fileType       the file type
     * @return list of attachments
     */
    @Query("SELECT ma FROM MessageAttachment ma WHERE ma.message.conversation.id = :conversationId " +
            "AND ma.fileType = :fileType ORDER BY ma.createdAt DESC")
    List<MessageAttachment> findByConversationIdAndFileType(@Param("conversationId") Integer conversationId,
                                                            @Param("fileType") String fileType);

    /**
     * Count attachments in a conversation.
     *
     * @param conversationId the conversation ID
     * @return attachment count
     */
    @Query("SELECT COUNT(ma) FROM MessageAttachment ma WHERE ma.message.conversation.id = :conversationId")
    Long countByConversationId(@Param("conversationId") Integer conversationId);

    /**
     * Calculate total file size in a conversation.
     *
     * @param conversationId the conversation ID
     * @return total file size in bytes
     */
    @Query("SELECT COALESCE(SUM(ma.fileSize), 0) FROM MessageAttachment ma " +
            "WHERE ma.message.conversation.id = :conversationId")
    Long getTotalFileSizeByConversationId(@Param("conversationId") Integer conversationId);
}