package com.tchalanet.server.core.drawresult.internal.read;

import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.drawresult.api.DrawResultProjection;
import com.tchalanet.server.core.drawresult.api.DrawResultProjectionCatalog;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class DrawResultProjectionCatalogImpl implements DrawResultProjectionCatalog {

  private final JdbcTemplate jdbc;
  private final JsonUtils jsonUtils;

  @Override
  public Optional<DrawResultProjection> findById(DrawResultId drawResultId) {
    return Optional.ofNullable(
        jdbc.query(
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
              and dr.deleted_at is null
            """,
            rs -> rs.next() ? mapRow(rs) : null,
            drawResultId.value()));
  }

  private DrawResultProjection mapRow(ResultSet rs) {
    try {
      var id = DrawResultId.of((UUID) rs.getObject("id"));
      String slotKey = rs.getString("slot_key");
      Instant occurredAt = rs.getTimestamp("occurred_at").toInstant();

      JsonNode haiti = parseJson(rs.getString("haiti_result"));
      JsonNode source = parseJson(rs.getString("source_result"));
      JsonNode payload = haiti != null && !haiti.isNull() ? haiti : source;

      return new DrawResultProjection(
          id,
          slotKey,
          occurredAt,
          text(payload, "lot1"),
          text(payload, "lot2"),
          text(payload, "lot3"),
          text(payload, "pick3"),
          list(payload, "two_digits"));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to map DrawResultProjection", e);
    }
  }

  private JsonNode parseJson(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      return jsonUtils.readValue(raw, JsonNode.class);
    } catch (Exception e) {
      return null;
    }
  }

  private static String text(JsonNode node, String key) {
    if (node == null) return null;
    JsonNode value = node.get(key);
    return value != null && value.isTextual() ? value.asText() : null;
  }

  private static List<String> list(JsonNode node, String key) {
    if (node == null) return List.of();
    JsonNode value = node.get(key);
    if (value == null || !value.isArray()) return List.of();
    List<String> out = new ArrayList<>();
    value.forEach(item -> {
      if (item != null && item.isTextual()) out.add(item.asText());
    });
    return out;
  }
}
