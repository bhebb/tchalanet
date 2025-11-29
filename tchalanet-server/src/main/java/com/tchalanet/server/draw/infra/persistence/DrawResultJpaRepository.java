package com.tchalanet.server.draw.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrawResultJpaRepository extends JpaRepository<DrawResultJpaEntity, UUID> {
  Optional<DrawResultJpaEntity> findByTenantIdAndDrawId(UUID tenantId, UUID drawId);
}
