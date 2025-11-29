package com.tchalanet.server.core.tenantconfig.infra.persistence.mapper;

import com.tchalanet.server.core.tenantconfig.domain.model.TenantSetting;
import com.tchalanet.server.core.tenantconfig.infra.persistence.entity.TenantSettingEntity;
import org.springframework.stereotype.Component;

@Component
public class TenantSettingMapper {

  public TenantSettingEntity toEntity(TenantSetting domain) {
    TenantSettingEntity entity = new TenantSettingEntity();
    entity.setId(domain.getId());
    entity.setTenantId(domain.getTenantId());
    entity.setConfigKey(domain.getConfigKey());
    entity.setConfigValue(domain.getConfigValue());
    entity.setConfigType(domain.getConfigType());
    entity.setActive(domain.isActive());
    // createdAt, updatedAt, version are handled by BaseTenantEntity
    return entity;
  }

  public TenantSetting toDomain(TenantSettingEntity entity) {
    return TenantSetting.load(
        entity.getId(),
        entity.getTenantId(),
        entity.getConfigKey(),
        entity.getConfigValue(),
        entity.getConfigType(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
