package com.tchalanet.server.catalog.resultslot.internal.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.internal.port.out.ResultSlotReaderPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResultSlotJpaAdapter implements ResultSlotReaderPort {

  private final JdbcTemplate jdbc;

  private static final RowMapper<ResultSlotView> MAPPER =
      new RowMapper<>() {
        @Override
        public ResultSlotView mapRow(ResultSet rs, int rowNum) throws SQLException {
          var idRaw = (UUID) rs.getObject("id");
          var id = ResultSlotId.of(idRaw);
          var key = rs.getString("slot_key");
          var provider = rs.getString("provider");
          var tz = ZoneId.of(rs.getString("timezone"));
          var drawTime = rs.getObject("draw_time", LocalTime.class);
          boolean active = rs.getBoolean("active");

          var sourceCfg = readJson(rs, "source_cfg");
          var projectionCfg = readJson(rs, "projection_cfg");

          return new ResultSlotView(
              id, key, provider, tz, drawTime, active, sourceCfg, projectionCfg);
        }

        private JsonNode readJson(ResultSet rs, String col) throws SQLException {
          Object raw = rs.getObject(col);
          if (raw == null) {
            return null;
          }
          try {
            return new ObjectMapper().readTree(raw.toString());
          } catch (Exception e) {
            return null;
          }
        }
      };

  @Override
  public Optional<ResultSlotView> findBySlotKey(String key) {
    var k = normalizeKey(key);
    if (k.isBlank()) {
      return Optional.empty();
    }

    var sql =
        """
                select id, slot_key, provider, timezone, draw_time, active, source_cfg, projection_cfg
                from result_slot
                where slot_key = ?
                and deleted_at is null
                and active = true
                limit 1
                """;

    var rows = jdbc.query(sql, MAPPER, k);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  @Override
  public List<ResultSlotView> listActive() {
    var sql =
        """
                select id, slot_key, provider, timezone, draw_time, active, source_cfg, projection_cfg
                from result_slot
                where active = true
                and deleted_at is null
                order by sort_order asc, slot_key asc
                """;
    return jdbc.query(sql, MAPPER);
  }

  @Override
  public Optional<ResultSlotView> findById(UUID id) {
    if (id == null) return Optional.empty();
    var sql =
        """
                select id, slot_key, provider, timezone, draw_time, active, source_cfg, projection_cfg
                from result_slot
                where id = ?
                and deleted_at is null
                and active = true
                limit 1
                """;

    var rows = jdbc.query(sql, MAPPER, id);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  private static String normalizeKey(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }
}
