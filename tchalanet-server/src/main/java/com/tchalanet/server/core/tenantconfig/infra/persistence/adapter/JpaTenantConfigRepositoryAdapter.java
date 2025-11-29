package com.tchalanet.server.core.tenantconfig.infra.persistence.adapter;

import com.tchalanet.server.core.tenantconfig.domain.model.TenantSetting;
import com.tchalanet.server.core.tenantconfig.domain.ports.out.TenantConfigRepositoryPort;
import com.tchalanet.server.core.tenantconfig.infra.persistence.entity.TenantSettingEntity;
import com.tchalanet.server.core.tenantconfig.infra.persistence.mapper.TenantSettingMapper;
import com.tchalanet.server.core.tenantconfig.infra.persistence.repository.SpringTenantSettingJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTenantConfigRepositoryAdapter implements TenantConfigRepositoryPort {

  private final SpringTenantSettingJpaRepository jpaRepository;
  private final TenantSettingMapper mapper;

  @Override
  public TenantSetting save(TenantSetting setting) {
    TenantSettingEntity entity = mapper.toEntity(setting);
    TenantSettingEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<TenantSetting> findById(UUID settingId) {
    return jpaRepository.findById(settingId).map(mapper::toDomain);
  }

  @Override
  public Optional<TenantSetting> findByTenantIdAndConfigKey(UUID tenantId, String configKey) {
    return jpaRepository.findByTenantIdAndConfigKey(tenantId, configKey).map(mapper::toDomain);
  }
}
