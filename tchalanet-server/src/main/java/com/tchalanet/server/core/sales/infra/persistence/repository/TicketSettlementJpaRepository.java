package com.tchalanet.server.core.sales.infra.persistence.repository;

import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketSettlementJpaRepository extends JpaRepository<TicketEntity, UUID> {

  @EntityGraph(attributePaths = "lines")
  @Query("""
      select t
      from TicketEntity t
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
  List<TicketEntity> findBatchForDrawWithLines(
      @Param("drawId") UUID drawId,
      @Param("saleStatus") TicketSaleStatus saleStatus,
      @Param("resultStatus") TicketResultStatus resultStatus,
      @Param("afterCreatedAt") Instant afterCreatedAt,
      @Param("afterId") UUID afterId);
}
