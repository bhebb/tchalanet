package com.tchalanet.server.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity extends AuditableEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  @PrePersist
  void prePersistId() {
    if (id == null) id = UUID.randomUUID();
  }
}
