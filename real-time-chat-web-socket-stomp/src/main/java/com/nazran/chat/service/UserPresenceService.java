package com.nazran.chat.service;

import com.nazran.chat.dto.response.UserPresenceResponse;

import java.util.List;

public interface UserPresenceService {

    /**
     * Update user online status.
     *
     * @param userId the user ID
     * @param isOnline the online status
     * @param deviceInfo optional device information
     */
    void updateUserPresence(Integer userId, Boolean isOnline, String deviceInfo);

    /**
     * Get user presence status.
     *
     * @param userId the user ID
     * @return presence status
     */
    UserPresenceResponse getUserPresence(Integer userId);

    /**
     * Get all online users.
     *
     * @return list of online user presences
     */
    List<UserPresenceResponse> getOnlineUsers();

    /**
     * Get all online super admins.
     *
     * @return list of online super admin presences
     */
    List<UserPresenceResponse> getOnlineSuperAdmins();

    /**
     * Mark user as offline (called on disconnect).
     *
     * @param userId the user ID
     */
    void markUserOffline(Integer userId);
}
