package com.tchalanet.server.core.draw.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.common.paging.TchPageMapper;
import com.tchalanet.server.core.draw.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawSummaryViewMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawSummaryViewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
public class DrawSummaryPersistenceAdapter implements DrawSummaryReaderPort {

    private static final int DEFAULT_LOOKAHEAD_HOURS = 24;

    private final DrawSummaryViewRepository repo;
    private final DrawSummaryViewMapper mapper;
    private final Clock clock;
    private final TchContextResolver contextResolver;

    @Override
    public Optional<DrawSummary> findById(DrawId drawId) {
        Objects.requireNonNull(drawId, "drawId is required");

        var tenantId = currentTenantId();

        return repo.findByTenantIdAndDrawId(tenantId.value(), drawId.value())
            .map(mapper::toProjection);
    }

    @Override
    public DrawSummary getById(DrawId drawId) {
        return findById(drawId)
            .orElseThrow(() -> new EntityNotFoundException("Draw not found: " + drawId));
    }

    @Override
    public TchPage<DrawSummary> findByCriteria(DrawSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria, "criteria is required");
        Objects.requireNonNull(pageable, "pageable is required");

        var tenantId = currentTenantId();

        var page = repo.search(
            tenantId.value(),
            criteria.resultSlotId() == null ? null : criteria.resultSlotId().value(),
            criteria.status(),
            criteria.from(),
            criteria.to(),
            pageable
        );

        return TchPageMapper.map(page, mapper::toProjection);
    }

    @Override
    public TchPage<DrawSummary> listNext(DrawSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria, "criteria is required");
        Objects.requireNonNull(pageable, "pageable is required");

        var tenantId = currentTenantId();

        Instant now = clock.instant();

        int lookaheadHours =
            criteria.lookaheadHours() == null
                ? DEFAULT_LOOKAHEAD_HOURS
                : criteria.lookaheadHours();

        Instant until = now.plusSeconds((long) lookaheadHours * 3600);

        var page = repo.next(
            tenantId.value(),
            criteria.resultSlotId() == null ? null : criteria.resultSlotId().value(),
            now,
            until,
            List.of(DrawStatus.SCHEDULED, DrawStatus.OPEN),
            pageable
        );

        return TchPageMapper.map(page, mapper::toProjection);
    }

    @Override
    public TchPage<DrawSummary> listLatestWithResults(DrawSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria, "criteria is required");
        Objects.requireNonNull(pageable, "pageable is required");

        var tenantId = currentTenantId();

        var keys = criteria.resultSlotKeys();
        boolean empty = keys == null || keys.isEmpty();

        var page = repo.latestWithResults(
            tenantId.value(),
            keys,
            empty,
            pageable
        );

        return TchPageMapper.map(page, mapper::toProjection);
    }

    private TenantId currentTenantId() {
        return contextResolver.currentOrThrow().effectiveTenantIdRequired();
    }

}
