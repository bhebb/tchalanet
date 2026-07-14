package com.tchalanet.server.platform.identity.internal.handoff;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PortalHandoffJpaRepository extends JpaRepository<PortalHandoffJpaEntity, UUID> {

  @Modifying
  @Query(
      """
      update PortalHandoffJpaEntity h
      set h.consumedAt = :consumedAt
      where h.id = :handoffId
        and h.consumedAt is null
      """)
  int markConsumed(@Param("handoffId") UUID handoffId, @Param("consumedAt") Instant consumedAt);
}
