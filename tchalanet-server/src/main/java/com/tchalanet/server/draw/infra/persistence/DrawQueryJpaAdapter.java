package com.tchalanet.server.draw.infra.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.draw.domain.dto.DrawDto;
import com.tchalanet.server.draw.domain.dto.NextDrawDto;
import com.tchalanet.server.draw.domain.ports.DrawQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawQueryJpaAdapter implements DrawQueryPort {

  private final EntityManager em;
  private final ObjectMapper mapper;

  @Override
  public List<DrawDto> findResultedDrawsForLastDays(java.util.UUID tenantId, int days) {
    String sql =
        "SELECT d.id as id, d.draw_channel_id as channel_id, dc.code as channel_code, d.scheduled_at as scheduled_at "
            + "FROM draw d JOIN draw_channel dc ON d.draw_channel_id = dc.id "
            + "WHERE d.status = 'RESULTED' AND d.tenant_id = :tenantId AND d.scheduled_at >= (now() - (:days || ' days')::interval) "
            + "ORDER BY d.scheduled_at DESC";
    List<Tuple> tuples =
        em.createNativeQuery(sql, Tuple.class)
            .setParameter("tenantId", tenantId)
            .setParameter("days", days)
            .getResultList();

    return tuples.stream().map(this::tupleToDrawDto).collect(Collectors.toList());
  }

  @Override
  public Map<java.util.UUID, NextDrawDto> findNextDrawPerChannel(java.util.UUID tenantId) {
    String sql =
        "SELECT DISTINCT ON (dc.id) d.id as id, dc.id as channel_id, dc.code as channel_code, d.scheduled_at as scheduled_at, dc.cutoff_sec as cutoff_sec "
            + "FROM draw d JOIN draw_channel dc ON d.draw_channel_id = dc.id "
            + "WHERE d.scheduled_at > now() AND d.tenant_id = :tenantId AND dc.active = true "
            + "ORDER BY dc.id, d.scheduled_at ASC";

    List<Tuple> tuples =
        em.createNativeQuery(sql, Tuple.class).setParameter("tenantId", tenantId).getResultList();

    Map<UUID, NextDrawDto> result = new HashMap<>();
    long serverNow = System.currentTimeMillis();
    for (Tuple t : tuples) {
      UUID id = (UUID) t.get("id");
      UUID channelId = (UUID) t.get("channel_id");
      String channelCode = (String) t.get("channel_code");
      OffsetDateTime scheduledAt =
          ((java.sql.Timestamp) t.get("scheduled_at"))
              .toInstant()
              .atOffset(java.time.ZoneOffset.UTC);
      Integer cutoffSec =
          t.get("cutoff_sec") == null ? 0 : ((Number) t.get("cutoff_sec")).intValue();
      OffsetDateTime cutoffAt = scheduledAt.minusSeconds(cutoffSec);
      result.put(
          channelId, new NextDrawDto(id, channelId, channelCode, scheduledAt, cutoffAt, serverNow));
    }
    return result;
  }

  @Override
  public Map<UUID, List<DrawDto>> findLastNPerChannel(UUID tenantId, int perChannelCount) {
    // window query to get last N draws per channel
    String sql =
        "SELECT * FROM ("
            + " SELECT d.id as id, d.draw_channel_id as channel_id, dc.code as channel_code, d.scheduled_at as scheduled_at, "
            + " ROW_NUMBER() OVER (PARTITION BY d.draw_channel_id ORDER BY d.scheduled_at DESC) rn "
            + " FROM draw d JOIN draw_channel dc ON d.draw_channel_id = dc.id "
            + " WHERE d.status = 'RESULTED' AND d.tenant_id = :tenantId AND d.deleted_at IS NULL "
            + " ) sub WHERE rn <= :limit ORDER BY channel_id, scheduled_at DESC";

    List<Tuple> tuples =
        em.createNativeQuery(sql, Tuple.class)
            .setParameter("tenantId", tenantId)
            .setParameter("limit", perChannelCount)
            .getResultList();

    Map<UUID, List<DrawDto>> grouped = new LinkedHashMap<>();
    for (Tuple t : tuples) {
      DrawDto d = tupleToDrawDto(t);
      grouped.computeIfAbsent(d.channelId(), k -> new ArrayList<>()).add(d);
    }
    return grouped;
  }

  private DrawDto tupleToDrawDto(Tuple t) {
    UUID id = (UUID) t.get("id");
    UUID channelId = (UUID) t.get("channel_id");
    String channelCode = (String) t.get("channel_code");
    OffsetDateTime scheduledAt =
        t.get("scheduled_at") == null
            ? null
            : ((java.sql.Timestamp) t.get("scheduled_at"))
                .toInstant()
                .atOffset(java.time.ZoneOffset.UTC);
    OffsetDateTime resultAt =
        t.get("result_at") == null
            ? null
            : ((java.sql.Timestamp) t.get("result_at"))
                .toInstant()
                .atOffset(java.time.ZoneOffset.UTC);

    List<Integer> numbers = null;
    Map<String, Object> meta = null;
    try {
      Object numbersObj = t.get("numbers");
      if (numbersObj != null) {
        // numbers can be PG object or string - try to convert via ObjectMapper
        String numJson = numbersObj.toString();
        numbers = mapper.readValue(numJson, new TypeReference<List<Integer>>() {});
      }
    } catch (Exception e) {
      log.warn("Failed to parse numbers for draw {}: {}", id, e.getMessage());
    }

    try {
      Object metaObj = t.get("meta");
      if (metaObj != null) {
        String metaJson = metaObj.toString();
        meta = mapper.readValue(metaJson, new TypeReference<Map<String, Object>>() {});
      }
    } catch (Exception e) {
      log.warn("Failed to parse meta for draw {}: {}", id, e.getMessage());
    }

    return new DrawDto(id, channelId, channelCode, scheduledAt, resultAt, numbers, meta);
  }
}
