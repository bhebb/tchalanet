package com.tchalanet.server.draw.infra.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawSource;
import com.tchalanet.server.draw.domain.model.DrawStatus;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    try {
      // if draw exists, load entity and update fields to avoid overwriting manual flags
      var maybe = jpa.findById(draw.id());
      DrawJpaEntity e;
      if (maybe.isPresent()) {
        e = maybe.get();
        // Update fields converting domain -> entity types
        e.setGameCode(draw.gameCode());
        e.setScheduledAt(draw.scheduledAt());
        e.setCutoffSec(draw.cutoffSec());
        e.setStatus(draw.status() == null ? null : draw.status().name());
        // resultPayload: domain stores JSON string, entity stores Map
        if (draw.resultPayload() == null) {
          e.setResultPayload(null);
        } else {
          try {
            var map = mapper.readValue(draw.resultPayload(), Map.class);
            e.setResultPayload(map);
          } catch (Exception ex) {
            // fallback: store as single entry
            e.setResultPayload(Map.of("raw", draw.resultPayload()));
          }
        }
        e.setDrawSource(draw.drawSource() == null ? null : draw.drawSource().name());
        e.setSystemGenerated(Boolean.valueOf(draw.systemGenerated()));
        e.setLocked(Boolean.valueOf(draw.locked()));
        e.setUpdatedBy(null); // audit fields not yet modelled on domain Draw
      } else {
        e = new DrawJpaEntity();
        e.setId(draw.id());
        e.setTenantId(draw.tenantId());
        e.setDrawChannelId(draw.drawChannelId());
        e.setGameCode(draw.gameCode());
        e.setScheduledAt(draw.scheduledAt());
        e.setCutoffSec(draw.cutoffSec());
        e.setStatus(draw.status() == null ? null : draw.status().name());
        if (draw.resultPayload() == null) {
          e.setResultPayload(null);
        } else {
          try {
            var map = mapper.readValue(draw.resultPayload(), Map.class);
            e.setResultPayload(map);
          } catch (Exception ex) {
            e.setResultPayload(Map.of("raw", draw.resultPayload()));
          }
        }
        e.setDrawSource(draw.drawSource() == null ? null : draw.drawSource().name());
        e.setSystemGenerated(Boolean.valueOf(draw.systemGenerated()));
        e.setLocked(Boolean.valueOf(draw.locked()));
        e.setCreatedBy(null);
        e.setUpdatedBy(null);
      }

      var saved = jpa.save(e);
      return toDomain(saved);
    } catch (RuntimeException ex) {
      throw ex;
    }
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
      String payload = draw.resultPayload();
      int updated =
          jpa.insertIfNotExists(
              draw.id(),
              draw.tenantId(),
              draw.drawChannelId(),
              draw.gameCode(),
              draw.scheduledAt(),
              draw.cutoffSec(),
              draw.status() == null ? null : draw.status().name(),
              payload);
      return updated > 0;
    } catch (Exception e) {
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

  @Override
  public List<Draw> findScheduledDrawsPastCutoff(UUID tenantId, Instant now) {
    return List.of();
  }

  public boolean applyResultIfEditable(UUID drawId, String provider, Object payload) {
    var maybe = jpa.findById(drawId);
    if (maybe.isEmpty()) return false;
    DrawJpaEntity e = maybe.get();

    if (Boolean.TRUE.equals(e.getLocked())) return false;
    if ("RESULTED".equals(e.getStatus()) && "MANUAL_RESULT".equals(e.getDrawSource())) return false;

    e.setResultPayload((java.util.Map) payload);
    e.setStatus("RESULTED");
    e.setDrawSource("SYSTEM");
    e.setUpdatedAt(java.time.Instant.now());
    jpa.save(e);
    return true;
  }

  private Draw toDomain(DrawJpaEntity e) {
    try {
      DrawStatus status = e.getStatus() == null ? null : DrawStatus.valueOf(e.getStatus());
      DrawSource source = e.getDrawSource() == null ? null : DrawSource.valueOf(e.getDrawSource());
      String payloadJson = null;
      if (e.getResultPayload() != null) {
        try {
          payloadJson = mapper.writeValueAsString(e.getResultPayload());
        } catch (Exception ex) {
          payloadJson = e.getResultPayload().toString();
        }
      }
      return new Draw(
          e.getId(),
          e.getTenantId(),
          e.getDrawChannelId(),
          e.getGameCode(),
          e.getScheduledAt(),
          e.getCutoffSec() == null ? 0 : e.getCutoffSec(),
          status,
          source,
          payloadJson,
          Boolean.TRUE.equals(e.getSystemGenerated()),
          Boolean.TRUE.equals(e.getLocked()));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
