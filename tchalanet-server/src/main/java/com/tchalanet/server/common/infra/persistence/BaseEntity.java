package com.tchalanet.server.common.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;

  @CreatedBy
  @Column(name = "created_by")
  private UUID createdBy;

  @LastModifiedBy
  @Column(name = "updated_by")
  private UUID updatedBy;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version; // version technique d’optimistic locking

  @PrePersist
  protected void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  protected void preUpdate() {
    updatedAt = Instant.now();
  }

  @Transient
  public boolean isDeleted() {
    return deletedAt != null;
  }
}
