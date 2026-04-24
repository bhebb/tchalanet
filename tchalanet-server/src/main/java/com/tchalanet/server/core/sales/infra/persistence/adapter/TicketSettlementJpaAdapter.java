package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.sales.application.port.out.TicketSettlementPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.infra.persistence.mapper.TicketMapper;
import com.tchalanet.server.core.sales.infra.persistence.repository.TicketSettlementJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketSettlementJpaAdapter implements TicketSettlementPort {

    private final TicketSettlementJpaRepository repo;
    private final TicketMapper mapper;

    @Override
    public List<Ticket> findNextBatchForDraw(
        DrawId drawId,
        Instant afterCreatedAt,
        UUID afterId,
        int limit) {

        Instant cursorTime = afterCreatedAt != null ? afterCreatedAt : Instant.EPOCH;
        UUID cursorId = afterId != null ? afterId : new UUID(0L, 0L);

        int pageSize = Math.max(1, limit);

        return repo
            .findBatchForDrawWithLines(
                drawId.value(),
                TicketSaleStatus.SOLD,
                TicketResultStatus.NOT_RESULTED,
                cursorTime,
                cursorId)
            .stream()
            .limit(pageSize)
            .map(mapper::toDomain)
            .toList();
    }
}
