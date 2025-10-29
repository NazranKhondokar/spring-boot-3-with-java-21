package com.nazran.chat.entity;

import com.nazran.chat.enums.ConversationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a conversation between a customer and a super admin.
 * Tracks conversation status and metadata.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(schema = "chat", name = "conversations")
public class Conversation extends BaseEntityWithUpdate {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The customer participating in this conversation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /**
     * The super admin assigned to this conversation (nullable if unassigned).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "super_admin_id")
    private User superAdmin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private ConversationStatus status = ConversationStatus.OPEN;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    /**
     * One-to-many relationship with messages in this conversation.
     */
    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Message> messages = new HashSet<>();

    /**
     * One-to-many relationship with conversation participants.
     */
    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ConversationParticipant> participants = new HashSet<>();

    /**
     * One-to-many relationship with unread counts for this conversation.
     */
    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ConversationUnreadCount> unreadCounts = new HashSet<>();
}
