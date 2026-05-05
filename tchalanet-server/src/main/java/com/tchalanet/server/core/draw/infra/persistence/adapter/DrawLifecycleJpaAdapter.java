package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.ExistingDrawKey;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;
import com.tchalanet.server.core.draw.application.query.projection.OpenableDrawRow;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DrawLifecycleJpaAdapter implements DrawLifecyclePort {

    private final DrawJpaRepository repo;
    private final DrawMapper mapper;
    private final JdbcTemplate jdbc;

    @Override
    public List<OpenableDrawRow> findOpenable(
        Instant now,
        int limit,
        int openHorizonHours,
        int openLagHours
    ) {
        Objects.requireNonNull(now, "now is required");

        var tenantUuid = TchContext.get().tenantUuid();

        return repo.findOpenable(tenantUuid, now, limit, openHorizonHours, openLagHours).stream()
            .map(this::mapOpenableRow)
            .toList();
    }

    @Override
    public List<DueToCloseRow> findDueToClose(Instant now, int limit) {
        Objects.requireNonNull(now, "now is required");

        var tenantUuid = TchContext.get().tenantUuid();

        return repo.findDueToClose(tenantUuid, now, limit).stream()
            .map(projection -> new DueToCloseRow(
                TenantId.of(projection.getTenantId()),
                DrawId.of(projection.getDrawId()),
                Boolean.TRUE.equals(projection.getLocked())
            ))
            .toList();
    }

    private OpenableDrawRow mapOpenableRow(
        com.tchalanet.server.core.draw.infra.persistence.projection.OpenableDrawProjection projection) {
        TenantId tenantId = TenantId.of(projection.getTenantId());
        DrawId drawId = DrawId.of(projection.getDrawId());
        Boolean locked = projection.getLocked() == null ? Boolean.FALSE : projection.getLocked();
        Instant scheduledAt = projection.getScheduledAt();
        Instant cutoffAt = projection.getCutoffAt();
        // preserve existing shape of OpenableDrawRow
        return new OpenableDrawRow(tenantId, drawId, locked, scheduledAt, cutoffAt);
    }

    @Override
    public int bulkOpen(List<DrawId> drawIds, Instant now) {
        if (drawIds == null || drawIds.isEmpty()) return 0;
        UUID[] ids = drawIds.stream().map(DrawId::value).toArray(UUID[]::new);
        return repo.bulkOpen(ids, now);
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    @Override
    public int bulkInsert(List<NewDrawRow> rows) {
        if (rows == null || rows.isEmpty()) return 0;

        final String sql =
            "INSERT INTO draw ("
                + "id, tenant_id, draw_channel_id, "
                + "draw_date, scheduled_at, cutoff_at, "
                + "status, opened_at, closed_at, "
                + "system_generated, locked, "
                + "created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now()) "
                + "ON CONFLICT (tenant_id, draw_channel_id, draw_date) "
                + "WHERE deleted_at IS NULL DO NOTHING";

        final int chunkSize = 200;
        int created = 0;

        for (int start = 0; start < rows.size(); start += chunkSize) {
            int end = Math.min(rows.size(), start + chunkSize);
            List<NewDrawRow> chunk = rows.subList(start, end);

            int[] counts =
                jdbc.batchUpdate(
                    sql,
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            NewDrawRow row = chunk.get(i);

                            ps.setObject(1, row.drawId().value());
                            ps.setObject(2, row.tenantId().value());
                            ps.setObject(3, row.drawChannelId().value());

                            if (row.drawDate() != null) ps.setObject(4, row.drawDate());
                            else ps.setNull(4, Types.DATE);

                            // timestamptz: prefer setObject(Instant)
                            if (row.scheduledAt() != null) ps.setTimestamp(5, ts(row.scheduledAt()));
                            else ps.setNull(5, Types.TIMESTAMP);

                            if (row.cutoffAt() != null) ps.setTimestamp(6, ts(row.cutoffAt()));
                            else ps.setNull(6, Types.TIMESTAMP);

                            ps.setString(7, row.status());

                            if (row.openedAt() != null) ps.setTimestamp(8, ts(row.openedAt()));
                            else ps.setNull(8, Types.TIMESTAMP);

                            if (row.closedAt() != null) ps.setTimestamp(9, ts(row.closedAt()));
                            else ps.setNull(9, Types.TIMESTAMP);

                            ps.setBoolean(10, row.systemGenerated());
                            ps.setBoolean(11, row.locked());
                        }

                        @Override
                        public int getBatchSize() {
                            return chunk.size();
                        }
                    });

            for (int count : counts) created += count; // 1 insert, 0 conflict
        }

        return created;
    }

    @Override
    public Draw save(Draw draw) {
        var entity = repo.save(mapper.toEntity(draw));
        return mapper.toDomain(entity);
    }

    @Override
    public int bulkClose(List<DrawId> drawIds, Instant now) {
        if (drawIds == null || drawIds.isEmpty()) return 0;
        UUID[] ids = drawIds.stream().map(DrawId::value).toArray(UUID[]::new);
        return repo.bulkClose(ids, now);
    }

    @Override
    public Set<ExistingDrawKey> findExistingKeys(TenantId tenantId, LocalDate from, LocalDate to) {
        String sql =
            "SELECT draw_channel_id, draw_date FROM draw WHERE deleted_at IS NULL AND tenant_id = ? AND draw_date BETWEEN ? AND ?";
        RowMapper<ExistingDrawKey> mapper =
            (rs, rowNum) ->
                new ExistingDrawKey(
                    (UUID) rs.getObject("draw_channel_id"), rs.getDate("draw_date").toLocalDate());
        List<ExistingDrawKey> list = jdbc.query(sql, new Object[]{tenantId.value(), from, to}, mapper);
        return new HashSet<>(list);
    }
}
