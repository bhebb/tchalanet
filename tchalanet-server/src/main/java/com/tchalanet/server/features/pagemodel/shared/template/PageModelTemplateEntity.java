package com.tchalanet.server.features.pagemodel.shared.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "page_model_template",
    uniqueConstraints = {@jakarta.persistence.UniqueConstraint(columnNames = {"code"})})
@Getter
@Setter
@Audited
public class PageModelTemplateEntity extends BaseEntity {

  @Column(name = "code", nullable = false, length = 128)
  private String code;

  @Column(name = "tenantId")
  private UUID tenantId;

  @Column(name = "logical_id")
  private String logicalId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "label")
  private String label;

  @Column(name = "description")
  private String description;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "schema", columnDefinition = "jsonb", nullable = false)
  private JsonNode schema;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "model", columnDefinition = "jsonb", nullable = false)
  private JsonNode model;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(name = "is_system", nullable = false)
  private boolean isSystem;
}
