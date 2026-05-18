package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OfflineGrantJpaRepository extends JpaRepository<OfflineGrantJpaEntity, UUID> {

}
