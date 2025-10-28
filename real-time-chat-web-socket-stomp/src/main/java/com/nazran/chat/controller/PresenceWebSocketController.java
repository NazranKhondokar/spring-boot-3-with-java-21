package com.nazran.chat.controller;

import com.nazran.chat.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket controller for handling user presence updates.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PresenceWebSocketController {

    private final UserPresenceService userPresenceService;

    /**
     * Handle user presence update (heartbeat).
     * Client should send this periodically to maintain online status.
     *
     * @param payload   the presence data
     * @param principal the authenticated user
     */
    @MessageMapping("/presence/heartbeat")
    public void handlePresenceHeartbeat(
            @Payload Map<String, Object> payload,
            Principal principal) {

        log.debug("Presence heartbeat from user: {}", principal.getName());

        try {
            String firebaseUserId = principal.getName();
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            String deviceInfo = (String) payload.getOrDefault("deviceInfo", "unknown");

            // Update presence
            userPresenceService.updateUserPresence(userId, true, deviceInfo);

        } catch (Exception e) {
            log.error("Error handling presence heartbeat: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user going online explicitly.
     *
     * @param payload   the presence data
     * @param principal the authenticated user
     */
    @MessageMapping("/presence/online")
    public void handleUserOnline(
            @Payload Map<String, Object> payload,
            Principal principal) {

        log.info("User going online: {}", principal.getName());

        try {
            String firebaseUserId = principal.getName();
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            String deviceInfo = (String) payload.getOrDefault("deviceInfo", "web");

            userPresenceService.updateUserPresence(userId, true, deviceInfo);

        } catch (Exception e) {
            log.error("Error handling user online: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user going offline explicitly.
     *
     * @param principal the authenticated user
     */
    @MessageMapping("/presence/offline")
    public void handleUserOffline(Principal principal) {

        log.info("User going offline: {}", principal.getName());

        try {
            String firebaseUserId = principal.getName();
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            userPresenceService.markUserOffline(userId);

        } catch (Exception e) {
            log.error("Error handling user offline: {}", e.getMessage(), e);
        }
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private Integer getUserIdFromFirebaseUid(String firebaseUserId) {
        // TODO: Implement actual logic
        log.warn("TODO: Implement getUserIdFromFirebaseUid in PresenceController");
        return 1; // Replace with actual implementation
    }
}
