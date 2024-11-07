package com.nazran.mapstruct.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    @JsonIgnore
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_AT", updatable = false)
    protected Date createdAt;

    @JsonIgnore
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATED_AT")
    protected Date updatedAt;

    //default one, auto increment for each operation like update
    @Version
    @JsonIgnore
    @Column(name = "RECORD_VERSION")
    private Integer recordVersion;

    @JsonIgnore
    @Column(name = "RECORD_ID")
    private Integer recordId;

    @Column(name = "CREATOR", updatable = false)
    private Long createdBy;

    @Column(name = "UPDATER")
    private Long updatedBy;

    @PrePersist
    public void prePersist() {
        this.createdAt = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }
}
