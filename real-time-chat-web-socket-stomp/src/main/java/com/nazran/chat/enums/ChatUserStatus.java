package com.nazran.chat.enums;

import lombok.Getter;

@Getter
public enum ChatUserStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    SUSPENDED("SUSPENDED"),
    DELETED("DELETED");

    private final String label;

    ChatUserStatus(String label) {
        this.label = label;
    }
}