package com.nazran.chat.controller;

import com.nazran.chat.entity.User;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.UserRepository;
import com.nazran.chat.service.UserPresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WebSocket controller for handling user presence updates.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "User Presence", description = "User presence management API over WebSocket")
public class PresenceWebSocketController {

    private final UserPresenceService userPresenceService;
    private final UserRepository userRepository;

    /**
     * Handle user presence update (heartbeat).
     * Client should send this periodically to maintain online status.
     *
     * @param payload   the presence data
     * @param firebaseUserId the authenticated user's Firebase UID
     */
    @MessageMapping("/presence/heartbeat")
    @Operation(
            summary = "Send presence heartbeat",
            description = "Client sends periodic heartbeat to maintain online status. Should be sent every 30-60 seconds."
    )
    public void handlePresenceHeartbeat(
            @Parameter(description = "Presence data including device information")
            @Payload Map<String, Object> payload,
            @AuthenticationPrincipal String firebaseUserId) {

        log.debug("Presence heartbeat from user: {}", firebaseUserId);

        try {
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
     * @param firebaseUserId the authenticated user's Firebase UID
     */
    @MessageMapping("/presence/online")
    @Operation(
            summary = "Mark user as online",
            description = "Explicitly marks the user as online when they connect or become active."
    )
    public void handleUserOnline(
            @Parameter(description = "Presence data including device information")
            @Payload Map<String, Object> payload,
            @AuthenticationPrincipal String firebaseUserId) {

        log.info("User going online: {}", firebaseUserId);

        try {
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
     * @param firebaseUserId the authenticated user's Firebase UID
     */
    @MessageMapping("/presence/offline")
    @Operation(
            summary = "Mark user as offline",
            description = "Explicitly marks the user as offline when they disconnect or become inactive."
    )
    public void handleUserOffline(@AuthenticationPrincipal String firebaseUserId) {

        log.info("User going offline: {}", firebaseUserId);

        try {
            Integer userId = getUserIdFromFirebaseUid(firebaseUserId);

            userPresenceService.markUserOffline(userId);

        } catch (Exception e) {
            log.error("Error handling user offline: {}", e.getMessage(), e);
        }
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    /**
     * Retrieves user by Firebase User ID or throws an exception.
     *
     * @param firebaseUserId the Firebase user ID to search for
     * @return the found User entity
     * @throws CustomMessagePresentException if no user found with the Firebase ID
     */
    private Integer getUserIdFromFirebaseUid(String firebaseUserId) {
        User user = userRepository.findByFirebaseUserId(firebaseUserId)
                .orElseThrow(() -> new CustomMessagePresentException("User does not exist for this Firebase User Id"));
        return user.getId();
    }
}