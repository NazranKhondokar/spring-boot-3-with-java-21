package com.nazran.chat.repository;

import com.nazran.chat.entity.User;
import com.nazran.chat.enums.ChatUserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find a user by Firebase user ID.
     *
     * @param firebaseUserId the Firebase user ID
     * @return Optional containing the user if found
     */
    Optional<User> findByFirebaseUserId(String firebaseUserId);

    /**
     * Find a user by email.
     *
     * @param email the user email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by email (case-insensitive).
     *
     * @param email the user email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if a user exists by Firebase user ID.
     *
     * @param firebaseUserId the Firebase user ID
     * @return true if user exists
     */
    boolean existsByFirebaseUserId(String firebaseUserId);

    /**
     * Check if a user exists by email.
     *
     * @param email the user email
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by status.
     *
     * @param status   the user status
     * @param pageable pagination parameters
     * @return page of users
     */
    Page<User> findByStatus(ChatUserStatus status, Pageable pageable);

    /**
     * Find users by role name.
     *
     * @param roleName the role name
     * @return list of users with the specified role
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Find all super admins who are active.
     *
     * @return list of active super admins
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = 'SUPER_ADMIN' AND u.status = 'ACTIVE'")
    List<User> findAllActiveSuperAdmins();

    /**
     * Find users with specific roles and status.
     *
     * @param roleNames list of role names
     * @param status    user status
     * @return list of matching users
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames AND u.status = :status")
    List<User> findByRoleNamesAndStatus(@Param("roleNames") List<String> roleNames,
                                        @Param("status") ChatUserStatus status);

    /**
     * Search users by name or email.
     *
     * @param searchTerm the search term
     * @param pageable   pagination parameters
     * @return page of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
}