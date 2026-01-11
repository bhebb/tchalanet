package com.tchalanet.server.core.drawresult.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.api.DrawResultsSearchCriteria;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.internal.infra.persistence.mapper.DrawResultMapper;
import com.tchalanet.server.core.drawresult.internal.infra.persistence.repo.DrawResultJdbcRepository;
import com.tchalanet.server.core.drawresult.internal.infra.persistence.repo.DrawResultJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Read-only adapter for DrawResult using JDBC/JPA for reads. */
@Component
@RequiredArgsConstructor
public class DrawResultJdbcReaderAdapter implements DrawResultReaderPort {

  private final DrawResultJdbcRepository jdbcRepo;
  private final DrawResultJpaRepository jpaRepository;
  private final DrawResultMapper mapper;

  @Override
  public Optional<DrawResultId> findByResultSlotIdAndOccurredAt(
      ResultSlotId resultSlotId, Instant occurredAt) {
    if (resultSlotId == null || occurredAt == null) return Optional.empty();
    var id = jdbcRepo.findByResultSlotIdAndOccurredAt(resultSlotId.uuid(), occurredAt);
    return Optional.ofNullable(DrawResultId.nullableOf(id));
  }

  @Override
  public DrawResult getById(DrawResultId id) {
    var drawResult =
        jpaRepository
            .findById(id.uuid())
            .orElseThrow(() -> new EntityNotFoundException("DrawResult not found"));
    return mapper.toDomain(drawResult);
  }

  @Override
  public TchPage<DrawResult> findByCriteria(DrawResultsSearchCriteria criteria) {
    var pageable =
        criteria == null || criteria.pageable() == null
            ? org.springframework.data.domain.PageRequest.of(0, 20)
            : criteria.pageable();

    int page = pageable.getPageNumber();
    int size = pageable.getPageSize();

    var total =
        jdbcRepo.countByCriteria(
            criteria == null ? null : criteria.provider(),
            criteria == null ? null : criteria.slotKey(),
            criteria == null ? null : criteria.from(),
            criteria == null ? null : criteria.to());

    var rows =
        jdbcRepo.findByCriteria(
            criteria == null ? null : criteria.provider(),
            criteria == null ? null : criteria.slotKey(),
            criteria == null ? null : criteria.from(),
            criteria == null ? null : criteria.to(),
            size,
            page * size);

    var items = rows.stream().map(mapper::toDomain).toList();

    int totalPages = size > 0 ? (int) ((total + size - 1) / size) : 0;
    boolean last = (page >= (totalPages - 1));
    boolean hasNext = page < (totalPages - 1);
    boolean hasPrevious = page > 0;

    return TchPage.of(items, page, size, total, totalPages, last, hasNext, hasPrevious);
  }
}
