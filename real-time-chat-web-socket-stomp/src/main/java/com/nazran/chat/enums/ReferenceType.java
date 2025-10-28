package com.nazran.chat.enums;

import lombok.Getter;

/**
 * Enum representing types of file references used in the system.
 * Each enum value is associated with an integer label for identification.
 *
 * Example usages include user avatars and specialist document uploads.
 */
@Getter
public enum ReferenceType {

    CHAT_MESSAGE(0);

    private final Integer label;

    ReferenceType(Integer label) {
        this.label = label;
    }
}