package com.nazran.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntityWithUpdateAndDelete extends BaseEntity implements UpdatableEntity, SoftDeletableEntity {

    @JsonIgnore
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP AT TIME ZONE 'UTC'")
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
    }

    @Override
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
