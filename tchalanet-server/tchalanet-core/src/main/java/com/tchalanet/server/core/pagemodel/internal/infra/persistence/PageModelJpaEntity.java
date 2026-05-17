package com.tchalanet.server.core.pagemodel.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import tools.jackson.databind.JsonNode;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Nouvelle entité PageModel alignée sur le design 2025. */
@Entity
@Table(
    name = "page_model",
    uniqueConstraints = {
      @jakarta.persistence.UniqueConstraint(columnNames = {"tenant_id", "value"})
    })
@Getter
@Setter
@Audited
public class PageModelJpaEntity extends BaseTenantEntity {

  @Column(name = "value", nullable = false, length = 128)
  private String code;

  @Column(name = "logical_id")
  private String logicalId;

  @Column(name = "name", nullable = false)
  private String name;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "schema", columnDefinition = "jsonb", nullable = false)
  private JsonNode schema;

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "slug")
  private String slug;

  @Column(name = "schema_version", nullable = false)
  private Integer schemaVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "model", columnDefinition = "jsonb", nullable = false)
  private JsonNode model;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PageModelStatus status;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "template_id")
  private UUID templateId;

  @Column(name = "active", nullable = false)
  private boolean active = true;
}
