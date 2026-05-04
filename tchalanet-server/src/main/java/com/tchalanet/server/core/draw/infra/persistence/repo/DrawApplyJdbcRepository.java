package com.tchalanet.server.core.draw.infra.persistence.repo;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DrawApplyJdbcRepository {

    private final JdbcTemplate jdbc;

    public record AppliedRow(UUID drawId, UUID drawChannelId) {}

    public List<AppliedRow> attachResultBySlotReturning(
        UUID tenantId,
        LocalDate drawDate,
        UUID resultSlotId,
        UUID drawResultId,
        Instant now,
        boolean force
    ) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(drawDate, "drawDate is required");
        Objects.requireNonNull(resultSlotId, "resultSlotId is required");
        Objects.requireNonNull(drawResultId, "drawResultId is required");
        Objects.requireNonNull(now, "now is required");

        var sql = """
        update draw d
        set draw_result_id = ?,
            status = 'RESULTED',
            resulted_at = ?,
            result_source = 'AUTO',
            updated_at = ?
        from draw_channel dc
        where dc.id = d.draw_channel_id
          and d.tenant_id = ?
          and d.draw_date = ?
          and dc.result_slot_id = ?
          and d.deleted_at is null
          and dc.deleted_at is null
          and d.locked = false
          and d.status = 'CLOSED'
          and d.draw_result_id is null
        returning d.id, d.draw_channel_id
        """;

        var ts = Timestamp.from(now);

        return jdbc.query(
            sql,
            ps -> {
                int i = 1;
                ps.setObject(i++, drawResultId);
                ps.setTimestamp(i++, ts);
                ps.setTimestamp(i++, ts);
                ps.setObject(i++, tenantId);
                ps.setDate(i++, java.sql.Date.valueOf(drawDate));
                ps.setObject(i++, resultSlotId);
            },
            (rs, rowNum) -> new AppliedRow(
                (UUID) rs.getObject(1),
                (UUID) rs.getObject(2)
            )
        );
    }
}
