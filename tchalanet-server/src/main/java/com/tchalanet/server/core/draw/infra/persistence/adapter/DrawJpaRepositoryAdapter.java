package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawsQuery;
import com.tchalanet.server.core.draw.application.util.HaitiResultExtractors;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import com.tchalanet.server.core.drawresult.api.DrawResultCatalog;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawJpaRepositoryAdapter implements DrawReaderPort {

  private final DrawJpaRepository jpa;
  private final DrawMapper mapper;
  private final DrawResultCatalog drawResultCatalog;

  @Override
  public Optional<Draw> findById(DrawId drawId) {
    return jpa.findById(drawId.uuid()).map(mapper::toDomain);
  }

  @Override
  public List<Draw> findClosableDraws(TenantId tenantId, ZonedDateTime now) {
    Instant inst = now.toInstant();
    return jpa
        .findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse("CLOSED", inst)
        .stream()
        .filter(e -> tenantId == null || tenantId.uuid().equals(e.getTenantId()))
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Draw> findResultedUnsettled(TenantId tenantId, ZonedDateTime now) {
    return jpa
        .findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse(
            "RESULTED", now.toInstant())
        .stream()
        .filter(e -> tenantId == null || tenantId.uuid().equals(e.getTenantId()))
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Draw> findNext(GetNextDrawQuery query) {
    if (query == null || query.tenantId() == null || query.now() == null) return Optional.empty();

    UUID tenantUuid = query.tenantId().uuid();
    Instant from = query.now().toInstant();
    Instant to = Instant.ofEpochSecond(Long.MAX_VALUE);

    return jpa
        .findByTenantIdAndScheduledAtBetweenFetchChannelOrderByScheduledAt(tenantUuid, from, to)
        .stream()
        .filter(
            e -> {
              String code = query.channelCode();
              if (code == null || code.isBlank()) return true;
              var ch = e.getDrawChannel();
              return ch != null && code.equalsIgnoreCase(ch.getCode());
            })
        .min(Comparator.comparing(DrawJpaEntity::getScheduledAt))
        .map(mapper::toDomain);
  }

  @Override
  public List<DrawSummary> findByCriteria(DrawSearchCriteria criteria) {
    if (criteria == null || criteria.tenantId() == null) return List.of();

    UUID tenantUuid = criteria.tenantId().uuid();
    var fromDate = criteria.from();
    var toDate = criteria.to();

    ZoneId utc = ZoneId.of("UTC");

    Instant fromInstant =
        (fromDate == null) ? Instant.EPOCH : fromDate.atStartOfDay(utc).toInstant();

    Instant toInstant =
        (toDate == null)
            ? Instant.ofEpochSecond(Long.MAX_VALUE)
            : toDate.plusDays(1).atStartOfDay(utc).toInstant().minusNanos(1);

    var rows =
        jpa.findSummariesWithChannelAndResult(tenantUuid, fromInstant, toInstant).stream()
            .filter(
                e -> {
                  String code = criteria.channelCode();
                  if (code == null || code.isBlank()) return true;
                  var ch = e.getDrawChannel();
                  return ch != null && code.equalsIgnoreCase(ch.getCode());
                })
            .map(e -> toSummary(e, utc))
            .collect(Collectors.toList());

    if (rows.isEmpty()) return List.of();

    // rows already sorted by scheduledAt asc (query ORDER BY). Mark first as next
    var first = rows.get(0);
    rows.set(
        0,
        new DrawSummary(
            first.id(),
            first.channelCode(),
            first.channelName(),
            first.scheduledAt(),
            first.cutoffTime(),
            first.status(),
            true,
            first.active(),
            first.lastResult()));

    return rows;
  }

  private DrawSummary toSummary(DrawJpaEntity e, ZoneId fallbackZone) {
    var ch = e.getDrawChannel();

    String chCode = ch == null ? null : ch.getCode();
    String chName = ch == null ? null : (ch.getName() == null ? ch.getCode() : ch.getName());
    boolean active = ch != null && ch.isActive();

    // ✅ meilleur affichage: timezone du channel si dispo, sinon fallback
    ZoneId zone = fallbackZone;
    try {
      if (ch != null && ch.getTimezone() != null && !ch.getTimezone().isBlank()) {
        zone = ZoneId.of(ch.getTimezone());
      }
    } catch (Exception ignore) {
    }

    ZonedDateTime scheduled = ZonedDateTime.ofInstant(e.getScheduledAt(), zone);
    ZonedDateTime cutoff =
        e.getCutoffAt() == null ? null : ZonedDateTime.ofInstant(e.getCutoffAt(), zone);

    DrawStatus status = e.getStatus() == null ? DrawStatus.SCHEDULED : e.getStatus();

    // ✅ lastResult : depuis la relation draw.drawResult (pas de catalog)
    var last = List.<Integer>of();
    if (e.getDrawResultId() != null) {
      try {
        DrawResult dr = drawResultCatalog.getById(DrawResultId.of(e.getDrawResultId()));
        last = HaitiResultExtractors.lastPick3(dr.haitiResult());
      } catch (Exception ex) {
        // ignore missing/parse errors and keep empty list
      }
    }

    return new DrawSummary(
        DrawId.of(e.getId()), chCode, chName, scheduled, cutoff, status, false, active, last);
  }

  @Override
  public List<Draw> findNextForChannels(GetNextDrawsQuery query) {
    return List.of();
  }
}
