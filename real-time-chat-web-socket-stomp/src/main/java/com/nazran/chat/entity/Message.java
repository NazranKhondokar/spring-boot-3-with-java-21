package com.nazran.chat.entity;

import com.nazran.chat.enums.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a message in a conversation.
 * Stores message content, type, and read status.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(schema = "chat", name = "messages")
public class Message extends BaseEntityWithUpdate {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The conversation this message belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * The user who sent this message.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    /**
     * One-to-many relationship with message attachments.
     */
    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MediaStorage> attachments = new HashSet<>();
}