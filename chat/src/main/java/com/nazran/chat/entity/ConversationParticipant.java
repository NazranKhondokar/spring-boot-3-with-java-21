package com.nazran.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.time.OffsetDateTime;

/**
 * Tracks participants in a conversation.
 * Supports future group chat functionality.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        schema = "chat",
        name = "conversation_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_conversation_user_participant",
                columnNames = {"conversation_id", "user_id"}
        )
)
public class ConversationParticipant extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The conversation the user is participating in.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * The participating user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (this.joinedAt == null) {
            this.joinedAt = this.getCreatedAt();
        }
    }
}
