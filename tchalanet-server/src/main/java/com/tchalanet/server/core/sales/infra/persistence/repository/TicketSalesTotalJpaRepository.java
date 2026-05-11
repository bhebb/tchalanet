package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public interface TicketSalesTotalJpaRepository extends JpaRepository<TicketEntity, UUID> {

    @Query("""

            select coalesce(sum(t.totalAmount), 0)
        from TicketEntity t
        where t.tenantId = :tenantId
          and t.sessionId = :sessionId
          and t.saleStatus in :statuses
        """)
    BigDecimal sumTotalAmountBySession(
        UUID tenantId,
        UUID sessionId,
        Set<TicketSaleStatus> statuses);
}
