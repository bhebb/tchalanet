package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineSalesGrantJpaRepository extends JpaRepository<OfflineSalesGrantJpaEntity, UUID> {

  boolean existsByTenantIdAndSellerUserIdAndTerminalIdAndSalesSessionIdAndStatus(
      UUID tenantId,
      UUID sellerUserId,
      UUID terminalId,
      UUID salesSessionId,
      String status);
}
