package com.tchalanet.server.core.sales.infra.persistence.repository;

import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface TicketSettlementQueryRepository extends Repository<TicketEntity, UUID> {

  @Query(
      value =
          """
        select exists(
          select 1
          from ticket t
          where t.deleted_at is null
            and t.draw_id = :drawId
            and t.status = 'SOLD'
        )
      """,
      nativeQuery = true)
  boolean existsPending(@Param("drawId") UUID drawId);

  @Query(
      value =
          """
        select count(1)
        from ticket t
        where t.deleted_at is null
          and t.draw_id = :drawId
          and t.status = 'SOLD'
      """,
      nativeQuery = true)
  long countPending(@Param("drawId") UUID drawId);
}
