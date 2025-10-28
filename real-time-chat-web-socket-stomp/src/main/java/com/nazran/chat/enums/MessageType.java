package com.nazran.chat.enums;

import lombok.Getter;

@Getter
public enum MessageType {
    TEXT("TEXT"),
    FILE("FILE"),
    IMAGE("IMAGE"),
    SYSTEM("SYSTEM"),
    VIDEO("VIDEO"),
    AUDIO("AUDIO");

    private final String label;

    MessageType(String label) {
        this.label = label;
    }
}