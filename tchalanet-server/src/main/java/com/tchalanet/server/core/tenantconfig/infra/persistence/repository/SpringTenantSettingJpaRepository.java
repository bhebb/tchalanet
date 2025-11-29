package com.tchalanet.server.core.tenantconfig.infra.persistence.repository;

import com.tchalanet.server.core.tenantconfig.infra.persistence.entity.TenantSettingEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringTenantSettingJpaRepository extends JpaRepository<TenantSettingEntity, UUID> {
  Optional<TenantSettingEntity> findByTenantIdAndConfigKey(UUID tenantId, String configKey);
}
