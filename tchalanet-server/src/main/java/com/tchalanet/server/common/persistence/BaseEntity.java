package com.tchalanet.server.common.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@MappedSuperclass
@Getter
@Audited
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
