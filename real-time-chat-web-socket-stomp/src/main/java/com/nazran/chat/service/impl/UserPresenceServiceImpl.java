package com.nazran.chat.service.impl;

import com.nazran.chat.dto.response.UserPresenceResponse;
import com.nazran.chat.entity.User;
import com.nazran.chat.entity.UserPresence;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.UserPresenceRepository;
import com.nazran.chat.repository.UserRepository;
import com.nazran.chat.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of UserPresenceService.
 * Handles user online/offline status tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceServiceImpl implements UserPresenceService {

    private final UserPresenceRepository presenceRepository;
    private final UserRepository chatUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void updateUserPresence(Integer userId, Boolean isOnline, String deviceInfo) {
        log.info("Updating presence for user ID: {} to {}", userId, isOnline ? "online" : "offline");

        User user = chatUserRepository.findById(userId)
                .orElseThrow(() -> new CustomMessagePresentException("User not found"));

        UserPresence presence = presenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPresence newPresence = new UserPresence();
                    newPresence.setUser(user);
                    return newPresence;
                });

        presence.setIsOnline(isOnline);
        presence.setLastSeen(OffsetDateTime.now(ZoneOffset.UTC));
        if (deviceInfo != null) {
            presence.setDeviceInfo(deviceInfo);
        }

        presenceRepository.save(presence);

        log.info("User presence updated successfully");

        // Broadcast presence update via WebSocket
        broadcastPresenceUpdate(user, isOnline);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPresenceResponse getUserPresence(Integer userId) {
        UserPresence presence = presenceRepository.findByUserId(userId)
                .orElse(null);

        if (presence == null) {
            return UserPresenceResponse.builder()
                    .userId(userId)
                    .isOnline(false)
                    .build();
        }

        return mapToPresenceResponse(presence);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPresenceResponse> getOnlineUsers() {
        List<UserPresence> onlinePresences = presenceRepository.findByIsOnlineTrue();
        return onlinePresences.stream()
                .map(this::mapToPresenceResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPresenceResponse> getOnlineSuperAdmins() {
        List<UserPresence> onlineAdmins = presenceRepository.findOnlineByRoleName("SUPER_ADMIN");
        return onlineAdmins.stream()
                .map(this::mapToPresenceResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markUserOffline(Integer userId) {
        log.info("Marking user ID: {} as offline", userId);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        presenceRepository.updateOnlineStatus(userId, false, now);

        User user = chatUserRepository.findById(userId)
                .orElseThrow(() -> new CustomMessagePresentException("User not found"));

        // Broadcast offline status
        broadcastPresenceUpdate(user, false);
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private UserPresenceResponse mapToPresenceResponse(UserPresence presence) {
        String lastSeen = null;
        if (presence.getLastSeen() != null) {
            lastSeen = presence.getLastSeen().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

        return UserPresenceResponse.builder()
                .userId(presence.getUser().getId())
                .isOnline(presence.getIsOnline())
                .lastSeen(lastSeen)
                .deviceInfo(presence.getDeviceInfo())
                .build();
    }

    private void broadcastPresenceUpdate(User user, Boolean isOnline) {
        // Broadcast to all users
        messagingTemplate.convertAndSend(
                "/topic/presence",
                UserPresenceResponse.builder()
                        .userId(user.getId())
                        .isOnline(isOnline)
                        .lastSeen(OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .build()
        );
    }
}
