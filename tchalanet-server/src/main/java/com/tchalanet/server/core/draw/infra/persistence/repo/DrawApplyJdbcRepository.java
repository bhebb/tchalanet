package com.tchalanet.server.core.draw.infra.persistence.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DrawApplyJdbcRepository {

    private final JdbcTemplate jdbc;

    public record AppliedRow(UUID drawId, UUID drawChannelId) {}

    public List<AppliedRow> attachResultBySlotReturning(
        UUID tenantId, LocalDate drawDate, UUID resultSlotId, UUID drawResultId, Instant now, boolean force) {

        var sql =
            """
          update draw d
          set draw_result_id = ?,
              status = 'RESULTED',
              resulted_at = coalesce(d.resulted_at, ?),
              result_source = coalesce(d.result_source, 'AUTO'),
              updated_at = ?
          from draw_channel dc
          where dc.id = d.draw_channel_id
            and d.tenant_id = ?
            and d.draw_date = ?
            and dc.result_slot_id = ?
            and d.deleted_at is null and dc.deleted_at is null
            and d.locked = false
            and d.status = 'CLOSED'
            and (
                 d.draw_result_id is null
                 or (? = true and d.draw_result_id is distinct from ?)
            )
          returning d.id, d.draw_channel_id
          """;

        Timestamp ts = Timestamp.from(now == null ? Instant.now() : now);

        return jdbc.query(
            sql,
            ps -> {
                int i = 1;
                ps.setObject(i++, drawResultId);
                ps.setTimestamp(i++, ts);
                ps.setString(i++, ts.toString()); // fallback if driver complains; replace with setTimestamp if needed
                // NOTE: Prefer setTimestamp; kept defensive for some drivers. If your driver is standard PG, use setTimestamp.
                ps.setObject(i++, tenantId);
                ps.setObject(i++, java.sql.Date.valueOf(drawDate));
                ps.setObject(i++, resultSlotId);
                ps.setBoolean(i++, force);
                ps.setObject(i++, drawResultId);
            },
            (rs, rowNum) -> new AppliedRow((UUID) rs.getObject(1), (UUID) rs.getObject(2)));
    }
}
