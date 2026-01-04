package com.tchalanet.server.core.sales.infra.persistence.repo;

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
            and t.tenant_id = :tenantId
            and t.draw_id = :drawId
            and t.status = 'PENDING'
        )
      """,
      nativeQuery = true)
  boolean existsPending(@Param("tenantId") UUID tenantId, @Param("drawId") UUID drawId);

  @Query(
      value =
          """
        select count(1)
        from ticket t
        where t.deleted_at is null
          and t.tenant_id = :tenantId
          and t.draw_id = :drawId
          and t.status = 'PENDING'
      """,
      nativeQuery = true)
  long countPending(@Param("tenantId") UUID tenantId, @Param("drawId") UUID drawId);
}
