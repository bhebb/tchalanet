package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.core.sales.internal.infra.persistence.TicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public interface TicketSalesTotalJpaRepository extends JpaRepository<TicketJpaEntity, UUID> {

    @Query("""

            select coalesce(sum(t.totalAmount), 0)
        from TicketJpaEntity t
        where t.tenantId = :tenantId
          and t.sessionId = :sessionId
          and t.saleStatus in :statuses
        """)
    BigDecimal sumTotalAmountBySession(
        UUID tenantId,
        UUID sessionId,
        Set<TicketSaleStatus> statuses);
}
