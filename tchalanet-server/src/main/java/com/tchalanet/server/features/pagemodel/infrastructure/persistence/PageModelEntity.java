package com.tchalanet.server.features.pagemodel.infrastructure.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.*;
// Not needed if using Instant from BaseEntity
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "page_model")
@Getter
@Setter
public class PageModelEntity extends BaseEntity { // Extend BaseEntity

  // ID is inherited from BaseEntity
  // @Id
  // @GeneratedValue
  // private UUID id;

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "lang", nullable = false, length = 8)
  private String lang;

  @Column(name = "json", nullable = false, columnDefinition = "jsonb")
  private String json;

  // createdAt, updatedAt are inherited from BaseEntity
  // @Column(name = "created_at", nullable = false)
  // private OffsetDateTime createdAt;
  // @Column(name = "updated_at", nullable = false)
  // private OffsetDateTime updatedAt;
}
