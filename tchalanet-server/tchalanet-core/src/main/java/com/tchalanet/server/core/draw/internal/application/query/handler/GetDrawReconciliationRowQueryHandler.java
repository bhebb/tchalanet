package com.tchalanet.server.core.draw.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.draw.api.query.GetDrawReconciliationRowQuery;
import com.tchalanet.server.core.draw.api.query.ReconciliationDrawRow;
import com.tchalanet.server.core.draw.internal.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.internal.infra.persistence.repo.DrawJpaRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetDrawReconciliationRowQueryHandler
    implements QueryHandler<GetDrawReconciliationRowQuery, ReconciliationDrawRow> {

    private final DrawJpaRepository repository;

    @Override
    public ReconciliationDrawRow handle(GetDrawReconciliationRowQuery query) {
        return repository.findById(query.drawId().value())
            .map(this::toRow)
            .orElseThrow(() -> ProblemRest.notFound("draw.not_found", query.drawId()));
    }

    private ReconciliationDrawRow toRow(DrawJpaEntity draw) {
        return new ReconciliationDrawRow(
            TenantId.of(draw.getTenantId()),
            DrawId.of(draw.getId()),
            DrawChannelId.of(draw.getDrawChannelId()),
            draw.getDrawResultId() == null ? null : DrawResultId.of(draw.getDrawResultId()),
            draw.getDrawDate(),
            draw.getScheduledAt(),
            draw.getResultedAt(),
            draw.getStatus()
        );
    }
}
