package com.nazran.chat.entity;

import java.time.OffsetDateTime;

public interface UpdatableEntity {
    OffsetDateTime getUpdatedAt();
    void setUpdatedAt(OffsetDateTime updatedAt);
}
