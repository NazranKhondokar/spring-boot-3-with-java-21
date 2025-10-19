package com.nazran.chat.repository;

import com.nazran.chat.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserPresence entity operations.
 */
@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, Integer> {

    /**
     * Find presence by user ID.
     *
     * @param userId the user ID
     * @return Optional containing user presence if found
     */
    @Query("SELECT up FROM UserPresence up WHERE up.user.id = :userId")
    Optional<UserPresence> findByUserId(@Param("userId") Integer userId);

    /**
     * Find all online users.
     *
     * @return list of online user presences
     */
    List<UserPresence> findByIsOnlineTrue();

    /**
     * Find all offline users.
     *
     * @return list of offline user presences
     */
    List<UserPresence> findByIsOnlineFalse();

    /**
     * Update user online status.
     *
     * @param userId   the user ID
     * @param isOnline the online status
     * @param lastSeen the last seen timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE UserPresence up SET up.isOnline = :isOnline, up.lastSeen = :lastSeen " +
            "WHERE up.user.id = :userId")
    int updateOnlineStatus(@Param("userId") Integer userId,
                           @Param("isOnline") Boolean isOnline,
                           @Param("lastSeen") OffsetDateTime lastSeen);

    /**
     * Find online users by role.
     *
     * @param roleName the role name
     * @return list of online user presences
     */
    @Query("SELECT up FROM UserPresence up JOIN up.user u JOIN u.roles r " +
            "WHERE r.name = :roleName AND up.isOnline = true")
    List<UserPresence> findOnlineByRoleName(@Param("roleName") String roleName);

    /**
     * Count online users.
     *
     * @return count of online users
     */
    Long countByIsOnlineTrue();

    /**
     * Check if user is online.
     *
     * @param userId the user ID
     * @return true if user is online
     */
    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END FROM UserPresence up " +
            "WHERE up.user.id = :userId AND up.isOnline = true")
    boolean isUserOnline(@Param("userId") Integer userId);
}
