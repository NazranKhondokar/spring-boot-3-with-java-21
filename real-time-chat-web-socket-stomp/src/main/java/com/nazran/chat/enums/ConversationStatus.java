package com.nazran.chat.enums;

import lombok.Getter;

@Getter
public enum ConversationStatus {
    OPEN("OPEN"),
    ASSIGNED("ASSIGNED"),
    CLOSED("CLOSED");

    private final String label;

    ConversationStatus(String label) {
        this.label = label;
    }
}