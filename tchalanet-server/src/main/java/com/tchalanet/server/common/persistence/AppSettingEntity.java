package com.tchalanet.server.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "app_setting")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class AppSettingEntity extends BaseEntity {

  @Column(name = "level", nullable = false)
  private String level;

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "terminal_id")
  private UUID terminalId;

  @Column(name = "outlet_id")
  private UUID outletId;

  @Column(name = "namespace", nullable = false)
  private String namespace;

  @Column(name = "setting_key", nullable = false)
  private String settingKey;

  @Column(name = "value_type", nullable = false)
  private String valueType;

  @Column(name = "setting_value", nullable = false)
  private String settingValue;

  @Column(name = "active", nullable = false)
  private Boolean active = true;
}
