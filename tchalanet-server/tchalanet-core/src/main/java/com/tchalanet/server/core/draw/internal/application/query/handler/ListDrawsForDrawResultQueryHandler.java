package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.query.ListDrawsForDrawResultQuery;
import com.tchalanet.server.core.draw.api.query.ReconciliationDrawRow;
import com.tchalanet.server.core.draw.internal.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.internal.infra.persistence.repo.DrawJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListDrawsForDrawResultQueryHandler
    implements QueryHandler<ListDrawsForDrawResultQuery, List<ReconciliationDrawRow>> {

    private final DrawJpaRepository repository;

    @Override
    public List<ReconciliationDrawRow> handle(ListDrawsForDrawResultQuery query) {
        return repository.findByDrawResultId(query.drawResultId().value()).stream()
            .map(this::toRow)
            .toList();
    }

    private ReconciliationDrawRow toRow(DrawJpaEntity draw) {
        return new ReconciliationDrawRow(
            TenantId.of(draw.getTenantId()),
            DrawId.of(draw.getId()),
            DrawChannelId.of(draw.getDrawChannelId()),
            DrawResultId.of(draw.getDrawResultId()),
            draw.getDrawDate(),
            draw.getScheduledAt(),
            draw.getResultedAt(),
            draw.getStatus()
        );
    }
}
