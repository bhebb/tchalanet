package com.tchalanet.server.catalog.i18n.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * I18n Override JPA Entity (INTERNAL)
 *
 * <p>This entity is INTERNAL to the catalog and MUST NOT be exposed outside the catalog module.
 *
 * <p>Use {@link com.tchalanet.server.catalog.i18n.api.I18nOverrideView} for public API.
 */
@Entity
@Table(name = "i18n_override")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class I18nOverrideEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "locale", nullable = false, length = 10)
  private String locale;

  @Column(name = "i18n_key", nullable = false, length = 255)
  private String i18nKey;

  @Column(name = "i18n_value", nullable = false, columnDefinition = "TEXT")
  private String i18nValue;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  /** Full key in format "locale:i18nKey" */
  public String fullKey() {
    return locale + ":" + i18nKey;
  }
}
