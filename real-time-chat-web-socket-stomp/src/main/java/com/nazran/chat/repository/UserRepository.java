package com.nazran.chat.repository;

import com.nazran.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
     * Find all super admins who are active.
     *
     * @return list of active super admins
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = 'SUPER_ADMIN' AND u.status = 'ACTIVE'")
    List<User> findAllActiveSuperAdmins();
}