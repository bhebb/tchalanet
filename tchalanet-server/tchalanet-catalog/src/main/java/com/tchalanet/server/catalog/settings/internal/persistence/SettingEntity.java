package com.tchalanet.server.catalog.settings.internal.persistence;

import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import com.tchalanet.server.common.persistence.BaseTenantEntity;

/**
 * Setting JPA Entity (INTERNAL)
 *
 * <p>This entity is INTERNAL to the catalog and MUST NOT be exposed outside the catalog module.
 *
 * <p>Use {@link SettingView} for public API.
 */
@Entity
@Table(name = "app_setting")
@Getter
@Setter
public class SettingEntity extends BaseTenantEntity {

  @Column(name = "outlet_id")
  private UUID outletId;

  @Column(name = "terminal_id")
  private UUID terminalId;

  @Column(name = "namespace", nullable = false, length = 255)
  private String namespace;

  @Column(name = "setting_key", nullable = false, length = 255)
  private String settingKey;

  @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
  private String settingValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "value_type", nullable = false, length = 50)
  private SettingValueType valueType;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false, length = 50)
  private SettingLevel level;

  @Enumerated(EnumType.STRING)
  @Column(name = "exposure", nullable = false, length = 50)
  private SettingExposure exposure = SettingExposure.INTERNAL;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  /** Full key in format "namespace.settingKey" */
  public String fullKey() {
    return namespace + "." + settingKey;
  }
}
