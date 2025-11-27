package com.tchalanet.server.tenantconfig.infra.persistence.entity;

import com.tchalanet.server.common.infra.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tenant_setting")
@Getter
@Setter
public class TenantSettingEntity extends BaseTenantEntity {

  @Id private UUID id;

  @Column(name = "config_key", nullable = false)
  private String configKey;

  @Column(name = "config_value", nullable = false)
  private String configValue;

  @Column(name = "config_type", nullable = false)
  private String configType;

  @Column(nullable = false)
  private boolean active;
}
