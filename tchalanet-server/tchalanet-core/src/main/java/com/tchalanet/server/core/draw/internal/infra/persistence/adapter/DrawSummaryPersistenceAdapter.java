package com.tchalanet.server.core.draw.internal.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.internal.infra.persistence.mapper.DrawSummaryViewMapper;
import com.tchalanet.server.core.draw.internal.infra.persistence.repo.DrawSummaryViewRepository;
import com.tchalanet.server.core.draw.internal.infra.persistence.view.DrawSummaryViewEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

        Specification<DrawSummaryViewEntity> spec =
            (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId.value());

        if (criteria.resultSlotId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("resultSlotId"), criteria.resultSlotId().value()));
        }

        if (criteria.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), criteria.status()));
        }

        if (criteria.from() != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("drawDate"), criteria.from()));
        }

        if (criteria.to() != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("drawDate"), criteria.to()));
        }

        var page = repo.findAll(spec, pageable);

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
