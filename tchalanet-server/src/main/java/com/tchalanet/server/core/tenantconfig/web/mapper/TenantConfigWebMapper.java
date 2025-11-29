package com.tchalanet.server.core.tenantconfig.web.mapper;

import com.tchalanet.server.core.tenantconfig.domain.model.TenantSetting;
import com.tchalanet.server.core.tenantconfig.domain.ports.in.UpsertTenantConfigUseCase;
import com.tchalanet.server.core.tenantconfig.web.dto.TenantSettingRequest;
import com.tchalanet.server.core.tenantconfig.web.dto.TenantSettingResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TenantConfigWebMapper {

  public UpsertTenantConfigUseCase.UpsertTenantConfigCommand toUpsertCommand(
      UUID tenantId, TenantSettingRequest request) {
    return new UpsertTenantConfigUseCase.UpsertTenantConfigCommand(
        request.id(),
        tenantId,
        request.configKey(),
        request.configValue(),
        request.configType(),
        request.active());
  }

  public TenantSettingResponse toTenantSettingResponse(TenantSetting domain) {
    return new TenantSettingResponse(
        domain.getId(),
        domain.getTenantId(),
        domain.getConfigKey(),
        domain.getConfigValue(),
        domain.getConfigType(),
        domain.isActive(),
        domain.getCreatedAt(),
        domain.getUpdatedAt());
  }
}
