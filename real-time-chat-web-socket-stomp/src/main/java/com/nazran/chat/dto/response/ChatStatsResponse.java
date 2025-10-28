package com.nazran.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStatsResponse {
    private Long totalConversations;
    private Long openConversations;
    private Long assignedConversations;
    private Long closedConversations;
    private Long unassignedConversations;
    private Long totalMessages;
    private Long totalUnreadMessages;
    private Long onlineUsers;
    private Long onlineSuperAdmins;
}
