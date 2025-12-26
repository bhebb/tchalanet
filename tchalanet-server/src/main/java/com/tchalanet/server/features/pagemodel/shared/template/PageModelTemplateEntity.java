package com.tchalanet.server.features.pagemodel.shared.template;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "page_model_template")
@Getter
@Setter
@Audited
public class PageModelTemplateEntity extends BaseEntity {

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "logical_id", nullable = false)
  private String logicalId;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "description")
  private String description;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @Column(name = "model", nullable = false, columnDefinition = "jsonb")
  private String modelJson;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(name = "is_system", nullable = false)
  private boolean isSystem;
}
