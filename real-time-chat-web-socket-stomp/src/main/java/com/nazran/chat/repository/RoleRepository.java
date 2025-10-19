package com.nazran.chat.repository;

import com.nazran.chat.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entity operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /**
     * Find a role by its name.
     *
     * @param name the role name
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Find a role by name (case-insensitive).
     *
     * @param name the role name
     * @return Optional containing the role if found
     */
    Optional<Role> findByNameIgnoreCase(String name);

    /**
     * Check if a role exists by name.
     *
     * @param name the role name
     * @return true if role exists
     */
    boolean existsByName(String name);

    /**
     * Find all roles excluding deleted ones.
     *
     * @return list of active roles
     */
    @Query("SELECT r FROM Role r WHERE r.deletedAt IS NULL")
    java.util.List<Role> findAllActive();
}
