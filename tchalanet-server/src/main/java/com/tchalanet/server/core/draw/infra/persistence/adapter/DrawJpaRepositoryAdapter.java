package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawChannelJpaRepository;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import jakarta.persistence.EntityManager;
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
public class DrawJpaRepositoryAdapter implements DrawReaderPort, DrawWriterPort {

  private final DrawJpaRepository jpa;
  private final DrawMapper mapper;
  private final DrawResultReaderPort drawResultReaderPort;
  private final EntityManager em;
  private final DrawChannelJpaRepository channelRepo;

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
  public List<DrawSummary> findByCriteria(DrawSearchCriteria drawSearchCriteria) {
    if (drawSearchCriteria == null || drawSearchCriteria.tenantId() == null) return List.of();

    UUID tenantUuid = drawSearchCriteria.tenantId().uuid();
    var fromDate = drawSearchCriteria.from();
    var toDate = drawSearchCriteria.to();
    // compute instants: from start of day UTC (inclusive), to end of day UTC (inclusive)
    ZoneId utc = ZoneId.of("UTC");
    Instant fromInstant =
        (fromDate == null) ? Instant.EPOCH : fromDate.atStartOfDay(utc).toInstant();
    Instant toInstant =
        (toDate == null)
            ? Instant.ofEpochSecond(Long.MAX_VALUE)
            : toDate.plusDays(1).atStartOfDay(utc).toInstant().minusNanos(1);

    var summaries =
        jpa
            .findByTenantIdAndScheduledAtBetweenFetchChannelOrderByScheduledAt(
                tenantUuid, fromInstant, toInstant)
            .stream()
            .filter(
                e -> {
                  String code = drawSearchCriteria.channelCode();
                  if (code == null || code.isBlank()) return true;
                  var ch = e.getDrawChannel();
                  return ch != null && code.equalsIgnoreCase(ch.getCode());
                })
            .map(
                e -> {
                  var ch = e.getDrawChannel();
                  String chCode = ch == null ? null : ch.getCode();
                  String chName =
                      ch == null ? null : (ch.getName() == null ? ch.getCode() : ch.getName());
                  ZonedDateTime scheduled = ZonedDateTime.ofInstant(e.getScheduledAt(), utc);
                  ZonedDateTime cutoff = null;
                  if (e.getCutoffSec() != null) {
                    cutoff = scheduled.minusSeconds(e.getCutoffSec());
                  }
                  boolean active = ch != null && Boolean.TRUE.equals(ch.getActive());
                  // lastResult loaded above if available
                  java.util.List<Integer> lastResult = java.util.List.of();
                  try {
                    if (drawResultReaderPort != null) {
                      var maybe =
                          drawResultReaderPort.findByDrawId(
                              TenantId.of(e.getTenantId()), DrawId.of(e.getId()));
                      if (maybe != null && maybe.isPresent()) {
                        var dr = maybe.get();
                        if (dr != null && dr.numbersMain() != null) {
                          var parsed =
                              dr.numbersMain().stream()
                                  .map(
                                      s -> {
                                        try {
                                          return Integer.valueOf(Integer.parseInt(s));
                                        } catch (Exception ex) {
                                          return null;
                                        }
                                      })
                                  .filter(java.util.Objects::nonNull)
                                  .toList();
                          if (!parsed.isEmpty()) lastResult = parsed;
                        }
                      }
                    }
                  } catch (Exception ex) {
                    // best-effort: ignore result loading failures and keep empty lastResult
                  }
                  boolean isNext = false; // will set later
                  DrawStatus status = e.getStatus() == null ? DrawStatus.SCHEDULED : e.getStatus();
                  return new DrawSummary(
                      new DrawId(e.getId()),
                      chCode,
                      chName,
                      scheduled,
                      cutoff,
                      status,
                      isNext,
                      active,
                      lastResult);
                })
            .sorted(Comparator.comparing(DrawSummary::scheduledAt))
            .collect(Collectors.toList());

    if (summaries.isEmpty()) return List.of();
    // mark the earliest scheduled draw as next
    int idx = 0; // already sorted ascending
    var first = summaries.get(idx);
    var marked =
        new DrawSummary(
            first.id(),
            first.channelCode(),
            first.channelName(),
            first.scheduledAt(),
            first.cutoffTime(),
            first.status(),
            true,
            first.active(),
            first.lastResult());
    summaries.set(idx, marked);
    return summaries;
  }

  @Override
  public List<Draw> findNextForChannels(GetNextDrawsQuery query) {
    return List.of();
  }

  @Override
  public Draw save(Draw draw) {
    var entity = mapper.toEntity(draw);
    ensureManagedRelations(entity);
    var saved = jpa.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public List<Draw> saveAll(List<Draw> draws) {
    var entities = draws.stream().map(mapper::toEntity).collect(Collectors.toList());
    entities.forEach(this::ensureManagedRelations);
    var saved = jpa.saveAll(entities);
    return saved.stream().map(mapper::toDomain).collect(Collectors.toList());
  }

  @Override
  public List<Draw> updateDraws(List<Draw> draws) {
    var entities = draws.stream().map(mapper::toEntity).collect(Collectors.toList());
    entities.forEach(this::ensureManagedRelations);
    var saved = jpa.saveAll(entities);
    return saved.stream().map(mapper::toDomain).collect(Collectors.toList());
  }

  private void ensureManagedRelations(DrawJpaEntity entity) {
    if (entity == null) return;
    var ch = entity.getDrawChannel();
    if (ch == null) return;

    // If ID present, prefer getReference
    if (ch.getId() != null) {
      try {
        var ref = em.getReference(DrawChannelJpaEntity.class, ch.getId());
        entity.setDrawChannel(ref);
        return;
      } catch (Exception ex) {
        // ignore and try fallback
      }
    }

    // Fallback: try to resolve by tenantId+code if available
    var tenantId = entity.getTenantId();
    var code = ch.getCode();
    if (tenantId != null && code != null && !code.isBlank()) {
      var opt = channelRepo.findByTenantIdAndCode(tenantId, code);
      if (opt.isPresent()) {
        var ref = em.getReference(DrawChannelJpaEntity.class, opt.get().getId());
        entity.setDrawChannel(ref);
        return;
      }
    }

    // Could not resolve channel => fail fast with descriptive error
    throw new IllegalStateException(
        "Cannot resolve DrawChannel for draw (tenantId="
            + entity.getTenantId()
            + ", code="
            + (code == null ? "<null>" : code)
            + ")");
  }
}
