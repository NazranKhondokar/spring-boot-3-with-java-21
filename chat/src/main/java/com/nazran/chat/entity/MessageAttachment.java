package com.nazran.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;

/**
 * Represents a file attachment associated with a message.
 * Stores file metadata and URL.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(schema = "chat", name = "message_attachments")
public class MessageAttachment extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The message this attachment belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;
}