package com.nazran.chat.entity;

import com.nazran.chat.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@Entity
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@Table(name = "media_storage")
public class MediaStorage extends BaseEntityWithUpdate {

    /**
     * The message this attachment belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "owner_id")
    private Integer ownerId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "reference_type", nullable = false)
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Integer referenceId;

    @Column(name = "external_id", nullable = false, length = 64, unique = true)
    private String externalId;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "mime_type", nullable = false, length = 64)
    private String mimeType;
}