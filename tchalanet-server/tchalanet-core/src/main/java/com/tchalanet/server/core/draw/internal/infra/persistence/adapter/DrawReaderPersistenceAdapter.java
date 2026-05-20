package com.tchalanet.server.core.draw.internal.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.internal.infra.persistence.mapper.DrawSummaryViewMapper;
import com.tchalanet.server.core.draw.internal.infra.persistence.repo.DrawJpaRepository;
import com.tchalanet.server.core.draw.internal.infra.persistence.repo.DrawSummaryViewRepository;
import com.tchalanet.server.core.draw.internal.domain.model.DrawStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DrawReaderPersistenceAdapter implements DrawReaderPort {

    private final DrawSummaryViewRepository summaryRepo;
    private final DrawJpaRepository drawRepo;
    private final DrawSummaryViewMapper mapper;
    private final Clock clock;
    private final TchContextResolver contextResolver;

    @Override
    public boolean existsSettledDrawForResult(DrawResultId drawResultId) {
        Objects.requireNonNull(drawResultId, "drawResultId is required");
        var tenantUuid = currentTenantUuid();

        return drawRepo.existsByTenantIdAndDrawResultIdAndStatusAndDeletedAtIsNull(
            tenantUuid,
            drawResultId.value(),
            DrawStatus.SETTLED
        );
    }

    @Override
    public List<DrawSummary> findByDrawResultId(DrawResultId drawResultId) {
        Objects.requireNonNull(drawResultId, "drawResultId is required");
        var tenantUuid = currentTenantUuid();

        return summaryRepo.findByTenantIdAndDrawResultId(
                tenantUuid,
                drawResultId.value()
            )
            .stream()
            .map(mapper::toProjection)
            .toList();
    }

    @Override
    public List<DrawSummary> findResultedWithProvisionalOlderThan(Duration duration) {
        Objects.requireNonNull(duration, "duration is required");
        var tenantUuid = currentTenantUuid();

        Instant threshold = clock.instant().minus(duration);

        return summaryRepo.findResultedProvisionalOlderThan(tenantUuid, threshold)
            .stream()
            .map(mapper::toProjection)
            .toList();
    }

    private UUID currentTenantUuid() {
        return contextResolver.currentOrThrow().effectiveTenantIdRequired().value();
    }
}
