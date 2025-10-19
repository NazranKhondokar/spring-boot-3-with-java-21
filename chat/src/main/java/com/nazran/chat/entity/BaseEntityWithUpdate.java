package com.nazran.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntityWithUpdate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP AT TIME ZONE 'UTC'")
    protected OffsetDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
    }
}
