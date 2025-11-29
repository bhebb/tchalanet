package com.tchalanet.server.core.tenantconfig.application;

import com.tchalanet.server.core.tenantconfig.domain.model.TenantSetting;
import com.tchalanet.server.core.tenantconfig.domain.ports.in.UpsertTenantConfigUseCase;
import com.tchalanet.server.core.tenantconfig.domain.ports.out.TenantConfigRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpsertTenantConfigService implements UpsertTenantConfigUseCase {

  private final TenantConfigRepositoryPort configRepository;

  @Override
  @Transactional
  public TenantSetting upsert(UpsertTenantConfigCommand command) {
    TenantSetting setting;
    if (command.id() != null) {
      setting =
          configRepository
              .findById(command.id())
              .orElseThrow(
                  () -> new IllegalArgumentException("Tenant Setting not found: " + command.id()));
      setting.update(command.configValue(), command.configType(), command.active());
      log.info("Updated Tenant Setting {} for tenant {}", setting.getId(), setting.getTenantId());
    } else {
      setting =
          TenantSetting.create(
              command.tenantId(), command.configKey(), command.configValue(), command.configType());
      log.info(
          "Created new Tenant Setting {} for tenant {}", setting.getId(), setting.getTenantId());
    }
    return configRepository.save(setting);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TenantSetting> getTenantSetting(UUID settingId) {
    return configRepository.findById(settingId);
  }
}
