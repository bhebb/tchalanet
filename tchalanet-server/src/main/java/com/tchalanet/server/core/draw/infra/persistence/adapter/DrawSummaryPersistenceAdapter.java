package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.draw.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawSummaryViewMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawSummaryViewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
public class DrawSummaryPersistenceAdapter implements DrawSummaryReaderPort {

    private final DrawSummaryViewRepository repo;
    private final DrawSummaryViewMapper mapper;

    @Override
    public Optional<DrawSummary> findById(DrawId drawId) {
        Objects.requireNonNull(drawId, "drawId is required");
        return repo.findByDrawId(drawId.value())
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

        var tenantId = TchContext.get().tenantId();

        var page = repo.search(
            tenantId.value(),
            criteria.resultSlotId() == null ? null : criteria.resultSlotId().value(),
            criteria.status() == null ? null : com.tchalanet.server.core.draw.domain.model.DrawStatus.valueOf(criteria.status()),
            criteria.from(),
            criteria.to(),
            pageable
        );

        return TchPageMapper.map(page, mapper::toProjection);
    }

    @Override
    public TchPage<DrawSummary> listNext(DrawSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria, "criteria is required");
        TenantId tenantId = TchContext.get().tenantId();
        Instant now = Instant.now();
        // Use lookahead hours if provided, otherwise 24h default
        Instant until = now.plusSeconds((long) (criteria.lookaheadHours() != null ? criteria.lookaheadHours() : 24) * 3600);

        var page = repo.next(
            tenantId.value(),
            criteria.resultSlotId() == null ? null : criteria.resultSlotId().value(),
            now,
            until,
            pageable
        );

        return TchPageMapper.map(page, mapper::toProjection);
    }

    @Override
    public TchPage<DrawSummary> listLatestWithResults(DrawSearchCriteria criteria, Pageable pageable) {
        Objects.requireNonNull(criteria, "criteria is required");
        TenantId tenantId = TchContext.get().tenantId();

        var page = repo.latestWithResults(
            tenantId.value(),
            criteria.resultSlotKeys(),
            criteria.resultSlotKeys() == null || criteria.resultSlotKeys().isEmpty(),
            pageable
        );

        return TchPageMapper.map(page, mapper::toProjection);
    }
}
