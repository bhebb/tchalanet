package com.tchalanet.server.core.draw.infra.persistence.adapter;

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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawLifecycleJpaAdapter implements DrawLifecyclePort {

  private final DrawJpaRepository repo;
  private final DrawMapper mapper;
  private final JdbcTemplate jdbc;

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
    // preserve existing shape of OpenableDrawRow
    return new OpenableDrawRow(tenantId, drawId, locked, scheduledAt, cutoffAt);
  }

  @Override
  public int bulkOpen(List<DrawId> drawIds) {
    if (drawIds == null || drawIds.isEmpty()) return 0;
    UUID[] ids = drawIds.stream().map(DrawId::value).toArray(UUID[]::new);
    return repo.bulkOpen(ids);
  }

  @Override
  public int bulkInsert(List<NewDrawRow> rows) {
    if (rows == null || rows.isEmpty()) return 0;

    final String sql =
        "INSERT INTO draw ("
            + "id, tenant_id, draw_channel_id, "
            + "draw_date, scheduled_at, cutoff_at, "
            + "cutoff_sec, status, draw_source, "
            + "system_generated, locked, "
            + "created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now()) "
            + "ON CONFLICT (tenant_id, draw_channel_id, draw_date) DO NOTHING";

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
                  NewDrawRow r = chunk.get(i);

                  ps.setObject(1, r.drawId().value());
                  ps.setObject(2, r.tenantId().value());
                  ps.setObject(3, r.drawChannelId().value());

                  if (r.drawDate() != null) ps.setObject(4, r.drawDate());
                  else ps.setNull(4, Types.DATE);

                  // timestamptz: prefer setObject(Instant)
                  if (r.scheduledAt() != null) ps.setObject(5, r.scheduledAt());
                  else ps.setNull(5, Types.TIMESTAMP_WITH_TIMEZONE);

                  if (r.cutoffAt() != null) ps.setObject(6, r.cutoffAt());
                  else ps.setNull(6, Types.TIMESTAMP_WITH_TIMEZONE);

                  Integer cutoffSec = null;
                  if (r.cutoffAt() != null && r.scheduledAt() != null) {
                    long diff = r.scheduledAt().getEpochSecond() - r.cutoffAt().getEpochSecond();
                    cutoffSec = (int) Math.max(0, diff);
                  }
                  if (cutoffSec == null) ps.setNull(7, Types.INTEGER);
                  else ps.setInt(7, cutoffSec);

                  ps.setString(8, r.status());

                  if (r.drawSource() != null) ps.setString(9, r.drawSource());
                  else ps.setNull(9, Types.VARCHAR);

                  ps.setBoolean(10, r.systemGenerated());
                  ps.setBoolean(11, r.locked());
                }

                @Override
                public int getBatchSize() {
                  return chunk.size();
                }
              });

      for (int c : counts) created += c; // 1 insert, 0 conflict
    }

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
    UUID[] ids = drawIds.stream().map(DrawId::value).toArray(UUID[]::new);
    return repo.bulkClose(ids);
  }

  @Override
  public Set<ExistingDrawKey> findExistingKeys(TenantId tenantId, LocalDate from, LocalDate to) {
    String sql =
        "SELECT draw_channel_id, draw_date FROM draw WHERE deleted_at IS NULL AND tenant_id = ? AND draw_date BETWEEN ? AND ?";
    RowMapper<ExistingDrawKey> mapper =
        (rs, rowNum) ->
            new ExistingDrawKey(
                (UUID) rs.getObject("draw_channel_id"), rs.getDate("draw_date").toLocalDate());
    List<ExistingDrawKey> list = jdbc.query(sql, new Object[] {tenantId.value(), from, to}, mapper);
    return new HashSet<>(list);
  }
}
