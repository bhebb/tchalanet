package com.tchalanet.server.core.tenantconfig.application;

import com.tchalanet.server.core.tenantconfig.domain.model.TenantSetting;
import com.tchalanet.server.core.tenantconfig.domain.ports.in.GetTenantConfigUseCase;
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
@Transactional(readOnly = true)
public class GetTenantConfigService implements GetTenantConfigUseCase {

  private final TenantConfigRepositoryPort configRepository;

  private Optional<TenantSetting> getActiveSetting(UUID tenantId, String configKey) {
    return configRepository
        .findByTenantIdAndConfigKey(tenantId, configKey)
        .filter(TenantSetting::isActive);
  }

  @Override
  public Optional<String> getString(UUID tenantId, String configKey) {
    return getActiveSetting(tenantId, configKey).map(TenantSetting::getConfigValue);
  }

  @Override
  public Optional<Boolean> getBoolean(UUID tenantId, String configKey) {
    return getActiveSetting(tenantId, configKey)
        .map(TenantSetting::getConfigValue)
        .map(Boolean::parseBoolean);
  }

  @Override
  public Optional<Integer> getInteger(UUID tenantId, String configKey) {
    return getActiveSetting(tenantId, configKey)
        .map(TenantSetting::getConfigValue)
        .map(Integer::parseInt);
  }
}
