package com.tchalanet.server.features.pagemodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

/** Nouvelle entité PageModel alignée sur le design 2025. */
@Entity
@Table(
    name = "page_model",
    uniqueConstraints = {
      @jakarta.persistence.UniqueConstraint(columnNames = {"tenantId", "code"})
    })
@Getter
@Setter
@Audited
public class PageModelEntity extends BaseTenantEntity {

  @Column(name = "code", nullable = false, length = 128)
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
  private PageStatus status;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "template_id")
  private UUID templateId;

  @Column(name = "active", nullable = false)
  private boolean active = true;
}
