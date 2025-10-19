package com.nazran.chat.entity;

import java.time.OffsetDateTime;

public interface SoftDeletableEntity {
    OffsetDateTime getDeletedAt();
    void setDeletedAt(OffsetDateTime deletedAt);
}
