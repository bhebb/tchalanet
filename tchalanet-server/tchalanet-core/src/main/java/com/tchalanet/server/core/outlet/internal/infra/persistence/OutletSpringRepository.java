package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutletSpringRepository
    extends JpaRepository<OutletJpaEntity, UUID>, JpaSpecificationExecutor<OutletJpaEntity> {

    Optional<OutletJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    int countByTenantIdAndStatus(UUID value, OutletStatus outletStatus);
}
