package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.TicketSaleStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketSettlementPort;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import com.tchalanet.server.core.sales.internal.infra.persistence.mapper.TicketMapper;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketSettlementJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
        var pageable = PageRequest.of(0, pageSize);

        return repo
            .findBatchForDrawWithLines(
                drawId.value(),
                TicketSaleStatus.SOLD,
                TicketResultStatus.NOT_RESULTED,
                cursorTime,
                cursorId,
                pageable)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
