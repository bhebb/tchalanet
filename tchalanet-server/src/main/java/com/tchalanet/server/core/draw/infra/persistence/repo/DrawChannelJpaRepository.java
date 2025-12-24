package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.entity.DrawChannelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrawChannelJpaRepository extends JpaRepository<DrawChannelJpaEntity, UUID> {

    Optional<DrawChannelJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<DrawChannelJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);

    List<DrawChannelJpaEntity> findByTenantIdOrderBySortOrderAsc(UUID tenantId);

    List<DrawChannelJpaEntity> findByActiveTrueOrderBySortOrder();

    List<DrawChannelJpaEntity> findByTenantIdAndActiveTrueOrderBySortOrderAsc(UUID tenantId);

    List<DrawChannelJpaEntity> findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);
}
