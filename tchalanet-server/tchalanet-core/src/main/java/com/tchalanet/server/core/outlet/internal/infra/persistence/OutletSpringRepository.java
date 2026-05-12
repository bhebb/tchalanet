package com.tchalanet.server.core.outlet.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutletSpringRepository
    extends JpaRepository<OutletJpaEntity, UUID>, JpaSpecificationExecutor<OutletJpaEntity> {
}
