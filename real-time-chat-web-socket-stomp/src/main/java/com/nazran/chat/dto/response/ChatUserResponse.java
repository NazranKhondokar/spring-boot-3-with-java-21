package com.nazran.chat.dto.response;

import com.nazran.chat.enums.ChatUserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserResponse {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean isEmailVerified;
    private String firebaseUserId;
    private ChatUserStatus status;
    private List<String> roles;
    private Boolean isOnline;
    private String lastSeen;
}