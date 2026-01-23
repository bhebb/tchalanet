package com.tchalanet.server.catalog.settings.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.catalog.settings.AppSettingCacheEvictListener;
import com.tchalanet.server.catalog.settings.AppSettingLevel;
import com.tchalanet.server.catalog.settings.AppSettingValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "app_setting")
@Audited
@Getter
@Setter
@EntityListeners({AppSettingCacheEvictListener.class})
public class AppSettingEntity extends BaseEntity {

  @Column(name = "tenantId")
  private UUID tenantId;

  @Column(name = "terminal_id")
  private UUID terminalId;

  @Column(name = "outlet_id")
  private UUID outletId;

  @Column(name = "namespace", nullable = false)
  private String namespace;

  @Column(name = "setting_key", nullable = false)
  private String settingKey;

  @Column(name = "setting_value", nullable = false)
  private String settingValue;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false)
  private AppSettingLevel level;

  @Enumerated(EnumType.STRING)
  @Column(name = "value_type", nullable = false)
  private AppSettingValueType valueType;
}
