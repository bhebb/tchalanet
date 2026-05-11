package com.tchalanet.server.core.offlinesync.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineSalesGrantJpaRepository extends JpaRepository<OfflineSalesGrantJpaEntity, UUID> {}

