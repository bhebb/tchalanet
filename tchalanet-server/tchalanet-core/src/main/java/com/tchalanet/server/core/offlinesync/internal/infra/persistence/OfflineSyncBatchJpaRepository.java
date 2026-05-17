package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineSyncBatchJpaRepository extends JpaRepository<OfflineSyncBatchJpaEntity, UUID> {}
