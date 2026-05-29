package com.tchalanet.server.core.outlet.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesZoneSpringRepository extends JpaRepository<SalesZoneJpaEntity, UUID> {

    Optional<SalesZoneJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<SalesZoneJpaEntity> findAllByTenantIdOrderByCodeAsc(UUID tenantId);
}
