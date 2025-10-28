package com.nazran.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceResponse {
    private Integer userId;
    private Boolean isOnline;
    private String lastSeen;
    private String deviceInfo;
}