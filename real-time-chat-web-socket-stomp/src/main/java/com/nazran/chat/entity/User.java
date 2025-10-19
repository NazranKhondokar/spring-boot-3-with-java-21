package com.nazran.chat.entity;

import com.nazran.chat.enums.ChatUserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a user in the chat system.
 * Stores personal details, authentication info, and role relationships.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(schema = "chat", name = "users")
public class User extends BaseEntityWithUpdate {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "email", length = 256, nullable = false, unique = true)
    @Email
    @Size(max = 256)
    private String email;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "firebase_user_id", length = 100, nullable = false, unique = true)
    private String firebaseUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatUserStatus status = ChatUserStatus.INACTIVE;

    /**
     * The roles assigned to this user.
     * Maintains a many-to-many relationship with the Role entity.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "chat",
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * One-to-many relationship with conversations where this user is the customer.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private Set<Conversation> customerConversations = new HashSet<>();

    /**
     * One-to-many relationship with conversations where this user is the super admin.
     */
    @OneToMany(mappedBy = "superAdmin", fetch = FetchType.LAZY)
    private Set<Conversation> adminConversations = new HashSet<>();

    /**
     * One-to-many relationship with messages sent by this user.
     */
    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY)
    private Set<Message> messages = new HashSet<>();

    /**
     * One-to-one relationship with user presence tracking.
     */
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserPresence userPresence;
}