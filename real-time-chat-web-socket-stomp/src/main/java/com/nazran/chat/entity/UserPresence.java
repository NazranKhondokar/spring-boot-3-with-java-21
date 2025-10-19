package com.nazran.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.time.OffsetDateTime;

/**
 * Tracks user presence (online/offline status) in the chat system.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(schema = "chat", name = "user_presence")
public class UserPresence extends BaseEntityWithUpdate {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The user whose presence is being tracked.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId
    private User user;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;
}