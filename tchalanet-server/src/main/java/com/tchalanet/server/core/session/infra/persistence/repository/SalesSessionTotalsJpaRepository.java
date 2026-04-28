package com.tchalanet.server.core.session.infra.persistence.repository;

import com.tchalanet.server.core.session.infra.persistence.entity.SalesSessionTotalsJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesSessionTotalsJpaRepository
    extends JpaRepository<SalesSessionTotalsJpaEntity, UUID> {

  Optional<SalesSessionTotalsJpaEntity> findByTenantIdAndSessionId(UUID tenantId, UUID sessionId);

  Optional<SalesSessionTotalsJpaEntity> findBySessionId(UUID sessionId);
}
