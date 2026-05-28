package com.tchalanet.server.core.drawresult.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultProjection;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultsCriteria;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.internal.infra.persistence.mapper.DrawResultMapper;
import com.tchalanet.server.core.drawresult.internal.infra.persistence.repo.DrawResultJdbcRepository;
import com.tchalanet.server.core.drawresult.internal.infra.persistence.repo.DrawResultJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DrawResultJdbcReaderAdapter implements DrawResultReaderPort {

    private final DrawResultJdbcRepository jdbcRepo;
    private final DrawResultJpaRepository jpaRepository;
    private final DrawResultMapper mapper;

    @Override
    public DrawResult getById(DrawResultId id) {
        var entity = jpaRepository.findById(id.value())
            .orElseThrow(() -> new EntityNotFoundException("DrawResult not found: " + id.value()));
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<DrawResultView> findViewById(DrawResultId id) {
        return id == null ? Optional.empty() : jdbcRepo.findViewById(id.value());
    }

    @Override
    public Optional<DrawResultView> findViewBySlotKeyAndOccurredAt(String slotKey, Instant occurredAt) {
        if (slotKey == null || slotKey.isBlank() || occurredAt == null) return Optional.empty();
        return jdbcRepo.findViewBySlotKeyAndOccurredAt(slotKey.trim().toUpperCase(), occurredAt);
    }

    @Override
    public Optional<DrawResultProjection> findProjectionById(DrawResultId id) {
        return id == null ? Optional.empty() : jdbcRepo.findProjectionById(id.value());
    }

    @Override
    public Optional<DrawResultProjection> findProjectionBySlotKeyAndOccurredAt(String slotKey, Instant occurredAt) {
        if (slotKey == null || slotKey.isBlank() || occurredAt == null) return Optional.empty();
        return jdbcRepo.findProjectionBySlotKeyAndOccurredAt(slotKey.trim().toUpperCase(), occurredAt);
    }

    @Override
    public Optional<DrawResultId> findByResultSlotIdAndOccurredAt(ResultSlotId resultSlotId, Instant occurredAt) {
        if (resultSlotId == null || occurredAt == null) return Optional.empty();
        return Optional.ofNullable(DrawResultId.nullableOf(
            jdbcRepo.findByResultSlotIdAndOccurredAt(resultSlotId.value(), occurredAt)
        ));
    }

    @Override
    public TchPage<DrawResultView> findViewsByCriteria(DrawResultsCriteria criteria) {
        var pageable = criteria == null || criteria.pageable() == null
            ? PageRequest.of(0, 20)
            : criteria.pageable();

        var page = pageable.getPageNumber();
        var size = pageable.getPageSize();

        var total = jdbcRepo.countByCriteria(
            criteria == null ? null : criteria.slotKey(),
            criteria == null ? null : criteria.status(),
            criteria == null ? null : criteria.quality(),
            criteria == null ? null : criteria.from(),
            criteria == null ? null : criteria.to()
        );

        var items = jdbcRepo.findViewsByCriteria(
            criteria == null ? null : criteria.slotKey(),
            criteria == null ? null : criteria.status(),
            criteria == null ? null : criteria.quality(),
            criteria == null ? null : criteria.from(),
            criteria == null ? null : criteria.to(),
            size,
            page * size
        );

        var totalPages = size > 0 ? (int) ((total + size - 1) / size) : 0;
        var last = totalPages == 0 || page >= totalPages - 1;

        return TchPage.of(items, page, size, total, totalPages, last, page < totalPages - 1, page > 0);
    }

    @Override
    public Optional<DrawResultView> findByDrawId(DrawId drawId) {
        return drawId == null ? Optional.empty() : jdbcRepo.findViewByDrawId(drawId.value());
    }

    @Override
    public boolean existsUsableExternalResult(ResultSlotId resultSlotId, Instant occurredAt) {
        return jdbcRepo.existsUsableExternalResult(resultSlotId.value(), occurredAt);
    }
}
