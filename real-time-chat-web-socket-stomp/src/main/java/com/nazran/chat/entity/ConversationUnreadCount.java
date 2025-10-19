package com.nazran.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;

/**
 * Caches unread message counts for conversations per user.
 * Optimizes performance by avoiding real-time counting queries.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        schema = "chat",
        name = "conversation_unread_count",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_conversation_user_unread",
                columnNames = {"conversation_id", "user_id"}
        )
)
public class ConversationUnreadCount extends BaseEntityWithUpdate {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The conversation for which unread count is tracked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * The user for whom unread count is tracked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount = 0;
}
