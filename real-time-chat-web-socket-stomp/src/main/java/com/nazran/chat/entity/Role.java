package com.nazran.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user role in the system.
 * <p>
 * Each role has a unique name and can be associated with multiple users.
 * This class extends {@link BaseEntityWithUpdateAndDelete} to inherit
 * common audit fields such as createdAt, updatedAt, and deletedAt.
 */
@Getter
@Setter
@Entity
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@Table(schema = "chat", name = "roles")
public class Role extends BaseEntityWithUpdateAndDelete {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "roles")
    private List<User> users = new ArrayList<>();
}