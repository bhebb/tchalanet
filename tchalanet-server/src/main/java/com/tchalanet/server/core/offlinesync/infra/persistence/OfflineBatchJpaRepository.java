package com.tchalanet.server.core.offlinesync.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineBatchJpaRepository extends JpaRepository<OfflineBatchJpaEntity, UUID> {}
