package com.nazran.chat.service.impl;

import com.nazran.chat.dto.request.AssignConversationRequest;
import com.nazran.chat.dto.request.CreateConversationRequest;
import com.nazran.chat.dto.request.MarkAsReadRequest;
import com.nazran.chat.dto.request.SendMessageRequest;
import com.nazran.chat.dto.response.*;
import com.nazran.chat.entity.*;
import com.nazran.chat.enums.ConversationStatus;
import com.nazran.chat.enums.MessageType;
import com.nazran.chat.exception.CustomMessagePresentException;
import com.nazran.chat.repository.ConversationRepository;
import com.nazran.chat.repository.ConversationUnreadCountRepository;
import com.nazran.chat.repository.MessageRepository;
import com.nazran.chat.repository.UserRepository;
import com.nazran.chat.service.ChatService;
import com.nazran.chat.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ChatService.
 * Handles core chat business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository chatUserRepository;
    private final ConversationUnreadCountRepository unreadCountRepository;
    private final UserPresenceService userPresenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request, Integer authenticatedUserId) {
        log.info("Creating conversation for customer ID: {}", request.getCustomerId());

        // Validate customer exists
        User customer = chatUserRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CustomMessagePresentException("Customer not found"));

        // Validate authenticated user is the customer
        if (!customer.getId().equals(authenticatedUserId)) {
            throw new CustomMessagePresentException("You can only create conversations for yourself");
        }

        // Check if customer already has an open conversation
        Optional<Conversation> existingConversation = conversationRepository
                .findMostRecentByCustomerId(customer.getId())
                .filter(c -> c.getStatus() != ConversationStatus.CLOSED);

        if (existingConversation.isPresent()) {
            log.info("Customer already has an active conversation: {}", existingConversation.get().getId());
            return mapToConversationResponse(existingConversation.get(), customer.getId());
        }

        // Create new conversation
        Conversation conversation = new Conversation();
        conversation.setCustomer(customer);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation = conversationRepository.save(conversation);

        // Create initial message
        Message initialMessage = new Message();
        initialMessage.setConversation(conversation);
        initialMessage.setSender(customer);
        initialMessage.setContent(request.getInitialMessage());
        initialMessage.setMessageType(MessageType.TEXT);
        initialMessage = messageRepository.save(initialMessage);

        // Initialize unread count for future admin
        conversation.setLastMessageAt(initialMessage.getCreatedAt());
        conversationRepository.save(conversation);

        log.info("Conversation created successfully with ID: {}", conversation.getId());

        // Notify all online super admins about new conversation
        notifyAvailableSuperAdmins(conversation);

        return mapToConversationResponse(conversation, customer.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getUserConversations(Integer userId, Pageable pageable) {
        log.info("Fetching conversations for user ID: {}", userId);

        User user = chatUserRepository.findById(userId)
                .orElseThrow(() -> new CustomMessagePresentException("User not found"));

        // Check if user is super admin or customer
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()));

        Page<Conversation> conversations;
        if (isSuperAdmin) {
            conversations = conversationRepository.findBySuperAdminIdOrderByLastMessageAtDesc(userId, pageable);
        } else {
            conversations = conversationRepository.findByCustomerIdOrderByLastMessageAtDesc(userId, pageable);
        }

        return conversations.map(conv -> mapToConversationResponse(conv, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(Integer conversationId, Integer authenticatedUserId) {
        log.info("Fetching conversation ID: {} for user ID: {}", conversationId, authenticatedUserId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new CustomMessagePresentException("Conversation not found"));

        // Validate user has access to this conversation
        validateConversationAccess(conversation, authenticatedUserId);

        return mapToConversationResponse(conversation, authenticatedUserId);
    }

    @Override
    @Transactional
    public ConversationResponse assignConversation(AssignConversationRequest request, Integer authenticatedUserId) {
        log.info("Assigning conversation ID: {} to super admin ID: {}",
                request.getConversationId(), request.getSuperAdminId());

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new CustomMessagePresentException("Conversation not found"));

        User superAdmin = chatUserRepository.findById(request.getSuperAdminId())
                .orElseThrow(() -> new CustomMessagePresentException("Super admin not found"));

        // Validate super admin role
        boolean isSuperAdmin = superAdmin.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()));

        if (!isSuperAdmin) {
            throw new CustomMessagePresentException("User is not a super admin");
        }

        conversation.setSuperAdmin(superAdmin);
        conversation.setStatus(ConversationStatus.ASSIGNED);
        conversation = conversationRepository.save(conversation);

        // Initialize unread count for super admin
        ConversationUnreadCount unreadCount = new ConversationUnreadCount();
        unreadCount.setConversation(conversation);
        unreadCount.setUser(superAdmin);
        unreadCount.setUnreadCount(0);
        unreadCountRepository.save(unreadCount);

        // Send system message
        Message systemMessage = new Message();
        systemMessage.setConversation(conversation);
        systemMessage.setSender(superAdmin);
        systemMessage.setContent("Conversation assigned to " + superAdmin.getFirstName() + " " + superAdmin.getLastName());
        systemMessage.setMessageType(MessageType.SYSTEM);
        systemMessage.setIsRead(true);
        messageRepository.save(systemMessage);

        log.info("Conversation assigned successfully");

        // Notify customer about assignment
        notifyConversationAssignment(conversation);

        return mapToConversationResponse(conversation, authenticatedUserId);
    }

    @Override
    @Transactional
    public ConversationResponse closeConversation(Integer conversationId, Integer authenticatedUserId) {
        log.info("Closing conversation ID: {}", conversationId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new CustomMessagePresentException("Conversation not found"));

        validateConversationAccess(conversation, authenticatedUserId);

        conversation.setStatus(ConversationStatus.CLOSED);
        conversation = conversationRepository.save(conversation);

        // Send system message
        User user = chatUserRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new CustomMessagePresentException("User not found"));

        Message systemMessage = new Message();
        systemMessage.setConversation(conversation);
        systemMessage.setSender(user);
        systemMessage.setContent("Conversation closed by " + user.getFirstName() + " " + user.getLastName());
        systemMessage.setMessageType(MessageType.SYSTEM);
        systemMessage.setIsRead(true);
        messageRepository.save(systemMessage);

        log.info("Conversation closed successfully");

        return mapToConversationResponse(conversation, authenticatedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getUnassignedConversations(Pageable pageable) {
        log.info("Fetching unassigned conversations");

        Page<Conversation> conversations = conversationRepository.findOpenUnassignedConversations(pageable);
        return conversations.map(conv -> mapToConversationResponse(conv, null));
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, Integer authenticatedUserId) {
        log.info("Sending message to conversation ID: {}", request.getConversationId());

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new CustomMessagePresentException("Conversation not found"));

        validateConversationAccess(conversation, authenticatedUserId);

        User sender = chatUserRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new CustomMessagePresentException("Sender not found"));

        // Create message
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setIsRead(false);
        message = messageRepository.save(message);

        // Update conversation last message timestamp
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        log.info("Message sent successfully with ID: {}", message.getId());

        // Broadcast message via WebSocket
        broadcastMessage(message);

        return mapToMessageResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationMessages(Integer conversationId, Integer authenticatedUserId, Pageable pageable) {
        log.info("Fetching messages for conversation ID: {}", conversationId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new CustomMessagePresentException("Conversation not found"));

        validateConversationAccess(conversation, authenticatedUserId);

        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return messages.map(this::mapToMessageResponse);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(MarkAsReadRequest request, Integer authenticatedUserId) {
        log.info("Marking messages as read in conversation ID: {}", request.getConversationId());

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new CustomMessagePresentException("Conversation not found"));

        validateConversationAccess(conversation, authenticatedUserId);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (request.getMessageId() != null) {
            // Mark specific message as read
            Message message = messageRepository.findById(request.getMessageId())
                    .orElseThrow(() -> new CustomMessagePresentException("Message not found"));

            if (!message.getIsRead()) {
                message.setIsRead(true);
                message.setReadAt(now);
                messageRepository.save(message);
            }
        } else {
            // Mark all unread messages as read
            messageRepository.markAllAsReadInConversation(
                    request.getConversationId(),
                    authenticatedUserId,
                    now
            );
        }

        // Reset unread count
        unreadCountRepository.resetUnreadCount(request.getConversationId(), authenticatedUserId);

        log.info("Messages marked as read successfully");

        // Broadcast read receipt via WebSocket
        broadcastReadReceipt(request.getConversationId(), authenticatedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalUnreadCount(Integer userId) {
        return unreadCountRepository.getTotalUnreadCountByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getConversationUnreadCount(Integer conversationId, Integer userId) {
        ConversationUnreadCount unreadCount = unreadCountRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElse(null);

        return UnreadCountResponse.builder()
                .conversationId(conversationId)
                .unreadCount(unreadCount != null ? unreadCount.getUnreadCount().longValue() : 0L)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> searchConversations(Integer userId, String searchTerm, Pageable pageable) {
        log.info("Searching conversations for user ID: {} with term: {}", userId, searchTerm);

        Page<Conversation> conversations = conversationRepository.findAllByUserId(userId, pageable);

        // Filter by search term (simplified - you can enhance this)
        return conversations.map(conv -> mapToConversationResponse(conv, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatStatsResponse getChatStats(Integer userId) {
        log.info("Fetching chat statistics for user ID: {}", userId);

        return ChatStatsResponse.builder()
                .totalConversations(conversationRepository.count())
                .openConversations(conversationRepository.countByStatus(ConversationStatus.OPEN))
                .assignedConversations(conversationRepository.countByStatus(ConversationStatus.ASSIGNED))
                .closedConversations(conversationRepository.countByStatus(ConversationStatus.CLOSED))
                .unassignedConversations(conversationRepository.countUnassignedOpen())
                .totalMessages(messageRepository.count())
                .totalUnreadMessages(unreadCountRepository.getTotalUnreadCountByUserId(userId))
                .onlineUsers((long) userPresenceService.getOnlineUsers().size())
                .onlineSuperAdmins((long) userPresenceService.getOnlineSuperAdmins().size())
                .build();
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private void validateConversationAccess(Conversation conversation, Integer userId) {
        boolean hasAccess = conversation.getCustomer().getId().equals(userId) ||
                (conversation.getSuperAdmin() != null && conversation.getSuperAdmin().getId().equals(userId));

        if (!hasAccess) {
            throw new CustomMessagePresentException("You don't have access to this conversation");
        }
    }

    private ConversationResponse mapToConversationResponse(Conversation conversation, Integer currentUserId) {
        // Get unread count for current user
        Long unreadCount = 0L;
        if (currentUserId != null) {
            ConversationUnreadCount unread = unreadCountRepository
                    .findByConversationIdAndUserId(conversation.getId(), currentUserId)
                    .orElse(null);
            unreadCount = unread != null ? unread.getUnreadCount().longValue() : 0L;
        }

        // Get last message
        MessageResponse lastMessage = null;
        List<Message> lastMessages = messageRepository.findLastMessageByConversationId(conversation.getId());
        if (!lastMessages.isEmpty()) {
            lastMessage = mapToMessageResponse(lastMessages.get(0));
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .customer(mapToUserResponse(conversation.getCustomer()))
                .superAdmin(conversation.getSuperAdmin() != null ? mapToUserResponse(conversation.getSuperAdmin()) : null)
                .status(conversation.getStatus())
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message) {
        User sender = message.getSender();
        String senderRole = getSenderRole(message.getConversation(), sender);

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(sender.getId())
                .senderName(sender.getFirstName() + " " + sender.getLastName())
                .senderRole(senderRole)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .attachments(new ArrayList<>()) // Populated by MessageService if needed
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    private ChatUserResponse mapToUserResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        UserPresenceResponse presence = userPresenceService.getUserPresence(user.getId());

        return ChatUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .isEmailVerified(user.getIsEmailVerified())
                .firebaseUserId(user.getFirebaseUserId())
                .status(user.getStatus())
                .roles(roleNames)
                .isOnline(presence != null ? presence.getIsOnline() : false)
                .lastSeen(presence != null ? presence.getLastSeen() : null)
                .build();
    }

    private String getSenderRole(Conversation conversation, User sender) {
        if (conversation.getCustomer().getId().equals(sender.getId())) {
            return "CUSTOMER";
        } else if (conversation.getSuperAdmin() != null &&
                conversation.getSuperAdmin().getId().equals(sender.getId())) {
            return "SUPER_ADMIN";
        }
        return "SYSTEM";
    }

    private void broadcastMessage(Message message) {
        // Broadcast to conversation topic
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getId(),
                mapToMessageResponse(message)
        );
    }

    private void broadcastReadReceipt(Integer conversationId, Integer userId) {
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("conversationId", conversationId);
        receipt.put("userId", userId);
        receipt.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC));

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/read",
                receipt
        );
    }

    private void notifyAvailableSuperAdmins(Conversation conversation) {
        List<User> onlineAdmins = chatUserRepository.findAllActiveSuperAdmins();

        for (User admin : onlineAdmins) {
            messagingTemplate.convertAndSendToUser(
                    admin.getFirebaseUserId(),
                    "/queue/new-conversation",
                    mapToConversationResponse(conversation, admin.getId())
            );
        }
    }

    private void notifyConversationAssignment(Conversation conversation) {
        // Notify customer
        messagingTemplate.convertAndSendToUser(
                conversation.getCustomer().getFirebaseUserId(),
                "/queue/conversation-assigned",
                mapToConversationResponse(conversation, conversation.getCustomer().getId())
        );
    }
}
