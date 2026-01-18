package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.core.sales.application.port.out.DrawResultViewPort;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawResultViewPortJdbcAdapter implements DrawResultViewPort {

  private final JdbcTemplate jdbc;
  private final ObjectMapper om;

  @Override
  public DrawResultView findById(UUID drawResultId) {
    return jdbc.query(
        """
        select
          dr.id,
          rs.key as slot_key,
          dr.occurred_at,
          dr.haiti_result,
          dr.source_result
        from draw_result dr
        join result_slot rs on rs.id = dr.result_slot_id
        where dr.id = ?
        """,
        rs -> rs.next() ? mapRow(rs) : null,
        drawResultId);
  }

  private DrawResultView mapRow(ResultSet rs) {
    try {
      UUID id = (UUID) rs.getObject("id");
      String slotKey = rs.getString("slot_key");
      Instant occurredAt = rs.getTimestamp("occurred_at").toInstant();

      JsonNode haiti = parseJson(rs.getString("haiti_result"));
      JsonNode source = parseJson(rs.getString("source_result"));

      // Prefer Haiti projection if present, else fallback to source
      JsonNode payload = (haiti != null && !haiti.isNull()) ? haiti : source;

      String pick3 = text(payload, "pick3");
      String pick4 = text(payload, "pick4");
      String pick5 = text(payload, "pick5");

      List<String> twoDigits = list(payload, "two_digits");

      return new DrawResultView(id, slotKey, occurredAt, pick3, pick4, pick5, twoDigits);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to map DrawResultView", e);
    }
  }

  private JsonNode parseJson(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      return om.readTree(raw);
    } catch (Exception e) {
      // safe fallback: treat as empty
      return null;
    }
  }

  private String text(JsonNode n, String key) {
    if (n == null) return null;
    JsonNode v = n.get(key);
    return v != null && v.isTextual() ? v.asText() : null;
  }

  private List<String> list(JsonNode n, String key) {
    if (n == null) return List.of();
    JsonNode v = n.get(key);
    if (v == null || !v.isArray()) return List.of();
    List<String> out = new ArrayList<>();
    v.forEach(item -> {
      if (item != null && item.isTextual()) out.add(item.asText());
    });
    return out;
  }
}

