package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;
import com.tchalanet.server.core.draw.application.query.projection.OpenableDrawRow;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawMapper;
import com.tchalanet.server.core.draw.infra.persistence.projection.DueToCloseProjection;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawLifecycleJpaAdapter implements DrawLifecyclePort {

  private final DrawJpaRepository repo;
  private final DrawMapper mapper;

  @Override
  public List<OpenableDrawRow> findOpenable(
      Instant now, int limit, int openHorizonHours, int openLagHours) {
    long nowEpoch = now == null ? Instant.now().getEpochSecond() : now.getEpochSecond();
    return repo.findOpenable(nowEpoch, limit, openHorizonHours, openLagHours).stream()
        .map(this::mapOpenableRow)
        .toList();
  }

  private OpenableDrawRow mapOpenableRow(
      com.tchalanet.server.core.draw.infra.persistence.projection.OpenableDrawProjection p) {
    TenantId tenantId = TenantId.of(p.getTenantId());
    DrawId drawId = DrawId.of(p.getDrawId());
    Boolean locked = p.getLocked() == null ? Boolean.FALSE : p.getLocked();
    Instant scheduledAt = p.getScheduledAt();
    Instant cutoffAt = p.getCutoffAt();
    return new OpenableDrawRow(tenantId, drawId, locked, scheduledAt, cutoffAt);
  }

  @Override
  public int bulkOpen(List<DrawId> drawIds) {
    if (drawIds == null || drawIds.isEmpty()) return 0;
    UUID[] ids = drawIds.stream().map(DrawId::uuid).toArray(UUID[]::new);
    return repo.bulkOpen(ids);
  }

  private DueToCloseRow mapDueToCloseRow(DueToCloseProjection p) {
    var tenantId = TenantId.of(p.getTenantId());
    var drawId = DrawId.of(p.getDrawId());
    var locked = p.getLocked() == null ? Boolean.FALSE : p.getLocked();
    return new DueToCloseRow(tenantId, drawId, locked);
  }

  private final JdbcTemplate jdbc;

  @Override
  public boolean existsByDate(TenantId tenantId, UUID drawChannelId, LocalDate drawDate) {
    return repo.existsByTenantIdAndDrawChannelIdAndDrawDateAndDeletedAtIsNull(
        tenantId.uuid(), drawChannelId, drawDate);
  }

  @Override
  public int bulkInsert(List<NewDrawRow> rows) {
    if (rows == null || rows.isEmpty()) return 0;

    // JDBC batch insert + ON CONFLICT DO NOTHING (Postgres)
    String sql =
        """
            INSERT INTO draw (
              id, tenant_id, draw_channel_id,
              draw_date, scheduled_at, cutoff_at,
              cutoff_sec, status, draw_source,
              system_generated, locked,
              created_at, updated_at
            )
            SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now()
            WHERE NOT EXISTS (
              SELECT 1 FROM draw d2
              WHERE d2.tenant_id = ? AND d2.draw_channel_id = ? AND d2.draw_date = ?
            )
            """;

    var rowsList = rows; // capture

    int[] counts =
        jdbc.batchUpdate(
            sql,
            new BatchPreparedStatementSetter() {
              @Override
              public void setValues(PreparedStatement ps, int i) throws SQLException {
                NewDrawRow r = rowsList.get(i);
                ps.setObject(1, r.drawId().uuid());
                ps.setObject(2, r.tenantId().uuid());
                ps.setObject(3, r.channelId().uuid());

                // draw_date -> DATE
                if (r.drawDate() != null) ps.setObject(4, r.drawDate());
                else ps.setNull(4, Types.DATE);

                // scheduled_at -> TIMESTAMP/TIMESTAMPTZ
                if (r.scheduledAt() != null) {
                  ps.setTimestamp(5, Timestamp.from(r.scheduledAt()));
                } else {
                  ps.setNull(5, Types.TIMESTAMP);
                }

                // cutoff_at -> TIMESTAMP/TIMESTAMPTZ
                if (r.cutoffAt() != null) {
                  ps.setTimestamp(6, Timestamp.from(r.cutoffAt()));
                } else {
                  ps.setNull(6, Types.TIMESTAMP);
                }

                // compute cutoff_sec as seconds difference
                Integer cutoffSec = null;
                if (r.cutoffAt() != null && r.scheduledAt() != null) {
                  long diff = r.scheduledAt().getEpochSecond() - r.cutoffAt().getEpochSecond();
                  cutoffSec = (int) Math.max(0, diff);
                }
                if (cutoffSec == null) ps.setNull(7, java.sql.Types.INTEGER);
                else ps.setInt(7, cutoffSec);
                ps.setString(8, r.status());
                ps.setString(9, r.drawSource());
                ps.setBoolean(10, r.systemGenerated());
                ps.setBoolean(11, r.locked());

                // duplicate keys for WHERE NOT EXISTS (tenant_id, draw_channel_id, draw_date)
                ps.setObject(12, r.tenantId().uuid());
                ps.setObject(13, r.channelId().uuid());
                if (r.drawDate() != null) ps.setObject(14, r.drawDate());
                else ps.setNull(14, Types.DATE);
              }

              @Override
              public int getBatchSize() {
                return rowsList.size();
              }
            });

    int created = 0;
    for (int c : counts) created += c; // Postgres renvoie 1 si insert, 0 si conflict
    return created;
  }

  @Override
  public Draw save(Draw draw) {
    var entity = repo.save(mapper.toEntity(draw));
    return mapper.toDomain(entity);
  }

  @Override
  public List<DueToCloseRow> findDueToClose(Instant now, int limit) {
    long nowEpoch = now == null ? Instant.now().getEpochSecond() : now.getEpochSecond();
    return repo.findDueToClose(nowEpoch, limit).stream()
        .map(
            proj ->
                new DueToCloseRow(
                    TenantId.of(proj.getTenantId()),
                    DrawId.of(proj.getDrawId()),
                    Boolean.TRUE.equals(proj.getLocked())))
        .toList();
  }

  @Override
  public int bulkClose(List<DrawId> drawIds) {
    if (drawIds == null || drawIds.isEmpty()) return 0;
    UUID[] ids = drawIds.stream().map(DrawId::uuid).toArray(UUID[]::new);
    return repo.bulkClose(ids);
  }
}
