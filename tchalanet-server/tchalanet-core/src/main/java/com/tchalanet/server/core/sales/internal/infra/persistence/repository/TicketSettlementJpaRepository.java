package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.core.sales.api.model.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import com.tchalanet.server.core.sales.internal.infra.persistence.TicketJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketSettlementJpaRepository extends JpaRepository<TicketJpaEntity, UUID> {

  @EntityGraph(attributePaths = "lines")
  @Query("""
      select t
      from TicketJpaEntity t
      where t.deletedAt is null
        and t.drawId = :drawId
        and t.saleStatus = :saleStatus
        and t.resultStatus = :resultStatus
        and (
          t.createdAt > :afterCreatedAt
          or (t.createdAt = :afterCreatedAt and t.id > :afterId)
        )
      order by t.createdAt asc, t.id asc
      """)
  List<TicketJpaEntity> findBatchForDrawWithLines(
      @Param("drawId") UUID drawId,
      @Param("saleStatus") TicketSaleStatus saleStatus,
      @Param("resultStatus") TicketResultStatus resultStatus,
      @Param("afterCreatedAt") Instant afterCreatedAt,
      @Param("afterId") UUID afterId,
      Pageable pageable);
}
