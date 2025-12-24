package com.tchalanet.server.core.session.infra.persistence.repository;

import com.tchalanet.server.core.session.infra.persistence.entity.PosSessionTotalsJpaEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosSessionTotalsJpaRepository extends JpaRepository<PosSessionTotalsJpaEntity, UUID> {

    Optional<PosSessionTotalsJpaEntity> findByTenantIdAndSessionId(UUID tenantId, UUID sessionId);

    Optional<PosSessionTotalsJpaEntity> findBySessionId(UUID sessionId);
}
