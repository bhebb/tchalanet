package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.api.DrawReaderPort;
import com.tchalanet.server.core.draw.application.util.HaitiResultExtractors;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawReaderPortAdapter implements DrawReaderPort {

  private final DrawJpaRepository repository;
  private final DrawResultReaderPort drawResultReader;
  private final JdbcTemplate jdbc;
  private final Clock clock; // [D9] Injected clock

  @Override
  public boolean existsSettledDrawForResult(DrawResultId drawResultId) {
    Objects.requireNonNull(drawResultId, "drawResultId is required");
    return repository.existsByDrawResultIdAndStatusAndDeletedAtIsNull(
        drawResultId.value(), DrawStatus.SETTLED);
  }

  @Override
  public List<DrawSummary> findByDrawResultId(DrawResultId drawResultId) {
    Objects.requireNonNull(drawResultId, "drawResultId is required");
    return repository.findByDrawResultIdAndDeletedAtIsNull(drawResultId.value())
        .stream()
        .map(this::toSummary)
        .collect(Collectors.toList());
  }

  @Override
  public List<DrawSummary> findResultedWithProvisionalOlderThan(Duration duration) {
    Instant threshold = Instant.now(clock).minus(duration); // [D9] Use injected clock

    String sql = """
        SELECT d.id
        FROM draw d
        JOIN draw_result dr ON dr.id = d.draw_result_id
        WHERE d.status = 'RESULTED'
          AND dr.status = 'PROVISIONAL'
          AND d.resulted_at < ?
          AND d.deleted_at IS NULL
        """;

    List<java.util.UUID> ids = jdbc.queryForList(sql, java.util.UUID.class, threshold);

    return repository.findAllById(ids).stream()
        .map(this::toSummary)
        .collect(Collectors.toList());
  }

  private DrawSummary toSummary(DrawJpaEntity e) {
    var ch = e.getDrawChannel();
    String chCode = ch == null ? null : ch.getCode();
    String chName = ch == null ? null : (ch.getName() == null ? ch.getCode() : ch.getName());
    boolean active = ch != null && ch.isActive();

    ZoneId zone = ZoneId.of("UTC");
    try {
      if (ch != null && ch.getTimezone() != null && !ch.getTimezone().isBlank()) {
        zone = ZoneId.of(ch.getTimezone());
      }
    } catch (Exception ignore) {}

    ZonedDateTime scheduled = ZonedDateTime.ofInstant(e.getScheduledAt(), zone);
    ZonedDateTime cutoff = e.getCutoffAt() == null ? null : ZonedDateTime.ofInstant(e.getCutoffAt(), zone);
    DrawStatus status = e.getStatus() == null ? DrawStatus.SCHEDULED : e.getStatus();

    var last = List.<Integer>of();
    if (e.getDrawResultId() != null) {
      try {
        DrawResult dr = drawResultReader.getById(DrawResultId.of(e.getDrawResultId()));
        last = HaitiResultExtractors.lastPick3(dr.haitiResult());
      } catch (Exception ex) {}
    }

    return new DrawSummary(
        DrawId.of(e.getId()), chCode, chName, scheduled, cutoff, status, false, active, last);
  }
}
