package com.tchalanet.server.draw.infra.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaDrawRepositoryAdapter implements DrawRepository {

  private final DrawJpaRepository jpa;
  private final ObjectMapper mapper;

  @Override
  public Optional<Draw> findById(UUID id) {
    return jpa.findById(id).map(this::toDomain);
  }

  @Override
  public Draw save(Draw draw) {
    // if draw exists, load entity and update fields to avoid overwriting manual flags
    // unintentionally
    var maybe = jpa.findById(draw.id());
    DrawJpaEntity e;
    if (maybe.isPresent()) {
      e = maybe.get();
      // steward fields: only update fields provided by domain Draw (we assume domain Draw contains
      // all fields)
      e.setGameCode(draw.gameCode());
      e.setScheduledAt(draw.scheduledAt());
      e.setCutoffSec(draw.cutoffSec());
      e.setStatus(draw.status());
      e.setResultPayload(draw.resultPayload());
      // respect existing drawSource/locked/systemGenerated unless explicitly set in domain
      if (draw.drawSource() != null) e.setDrawSource(draw.drawSource());
      if (draw.systemGenerated() != null) e.setSystemGenerated(draw.systemGenerated());
      if (draw.locked() != null) e.setLocked(draw.locked());
      e.setUpdatedBy(draw.updatedBy());
    } else {
      e = new DrawJpaEntity();
      e.setId(draw.id());
      e.setTenantId(draw.tenantId());
      e.setDrawChannelId(draw.drawChannelId());
      e.setGameCode(draw.gameCode());
      e.setScheduledAt(draw.scheduledAt());
      e.setCutoffSec(draw.cutoffSec());
      e.setStatus(draw.status());
      e.setResultPayload(draw.resultPayload());
      e.setDrawSource(draw.drawSource());
      e.setSystemGenerated(draw.systemGenerated());
      e.setLocked(draw.locked());
      e.setCreatedBy(draw.createdBy());
      e.setUpdatedBy(draw.updatedBy());
    }

    var saved = jpa.save(e);
    return toDomain(saved);
  }

  @Override
  public List<Draw> findByTenantAndScheduledAtBetween(UUID tenantId, Instant from, Instant to) {
    return jpa.findByTenantIdAndScheduledAtBetweenOrderByScheduledAt(tenantId, from, to).stream()
        .map(this::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public boolean existsByTenantChannelAndScheduledAt(
      UUID tenantId, UUID drawChannelId, Instant scheduledAt) {
    return jpa.existsByTenantIdAndDrawChannelIdAndScheduledAt(tenantId, drawChannelId, scheduledAt);
  }

  @Override
  public boolean saveIfNotExists(Draw draw) {
    try {
      String payload =
          draw.resultPayload() == null ? null : mapper.writeValueAsString(draw.resultPayload());
      int updated =
          jpa.insertIfNotExists(
              draw.id(),
              draw.tenantId(),
              draw.drawChannelId(),
              draw.gameCode(),
              draw.scheduledAt(),
              draw.cutoffSec(),
              draw.status(),
              payload);
      return updated > 0;
    } catch (Exception e) {
      // fallback to simple save on error
      boolean exists =
          existsByTenantChannelAndScheduledAt(
              draw.tenantId(), draw.drawChannelId(), draw.scheduledAt());
      if (exists) return false;
      save(draw);
      return true;
    }
  }

  // new helper for settlement: find closed draws scheduled before 'before' but excluding locked
  // ones
  public List<Draw> findClosedScheduledBefore(Instant before) {
    return jpa
        .findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse("CLOSED", before)
        .stream()
        .map(this::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Draw> findByStatusAndScheduledAtBefore(String status, Instant before) {
    return jpa.findByStatusAndScheduledAtBeforeAndDeletedAtIsNull(status, before).stream()
        .map(this::toDomain)
        .collect(Collectors.toList());
  }

  /**
   * Attempt to apply result payload to a draw if it's editable by system. Returns true if applied,
   * false if skipped (locked or manual result present).
   */
  public boolean applyResultIfEditable(UUID drawId, String provider, Object payload) {
    var maybe = jpa.findById(drawId);
    if (maybe.isEmpty()) return false;
    DrawJpaEntity e = maybe.get();

    // If locked or already RESULTED with manual source, skip
    if (Boolean.TRUE.equals(e.getLocked())) return false;
    if ("RESULTED".equals(e.getStatus()) && "MANUAL_RESULT".equals(e.getDrawSource())) return false;

    // apply result
    e.setResultPayload((java.util.Map) payload);
    e.setStatus("RESULTED");
    e.setDrawSource("SYSTEM");
    e.setUpdatedAt(java.time.Instant.now());
    jpa.save(e);
    return true;
  }

  private Draw toDomain(DrawJpaEntity e) {
    return new Draw(
        e.getId(),
        e.getTenantId(),
        e.getDrawChannelId(),
        e.getGameCode(),
        e.getScheduledAt(),
        e.getCutoffSec(),
        e.getStatus(),
        e.getResultPayload(),
        e.getDrawSource(),
        e.getSystemGenerated(),
        e.getLocked(),
        e.getCreatedBy(),
        e.getUpdatedBy());
  }
}
