package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.OpenableDrawRow;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawLifecycleJpaAdapter implements DrawLifecyclePort {

  private final DrawJpaRepository repo;

  @Override
  public List<OpenableDrawRow> findOpenable(
      Instant now, int limit, int openHorizonHours, int openLagHours) {
    long nowEpoch = now == null ? Instant.now().getEpochSecond() : now.getEpochSecond();
    return repo.findOpenable(nowEpoch, limit, openHorizonHours, openLagHours).stream()
        .map(this::mapOpenableRow)
        .toList();
  }

  private OpenableDrawRow mapOpenableRow(Object[] row) {
    TenantId tenantId = TenantId.of((UUID) row[0]);
    DrawId drawId = DrawId.of((UUID) row[1]);
    Boolean locked = row[2] == null ? Boolean.FALSE : (Boolean) row[2];
    Instant scheduledAt = row[3] == null ? null : (Instant) row[3];
    Integer cutoffSec = null;
    if (row.length > 4 && row[4] != null) {
      // sometimes cutoff may be a number type
      Object o = row[4];
      if (o instanceof Number) cutoffSec = ((Number) o).intValue();
      else cutoffSec = Integer.parseInt(o.toString());
    }
    return new OpenableDrawRow(
        tenantId, drawId, locked, scheduledAt, cutoffSec == null ? 0 : cutoffSec);
  }

  @Override
  public int bulkOpen(List<DrawId> drawIds) {
    if (drawIds == null || drawIds.isEmpty()) return 0;
    UUID[] ids = drawIds.stream().map(DrawId::uuid).toArray(UUID[]::new);
    return repo.bulkOpen(ids);
  }

  @Override
  public List<DueToCloseRow> findDueToClose(Instant now, int limit) {
    long nowEpoch = now == null ? Instant.now().getEpochSecond() : now.getEpochSecond();
    // repo returns Object[] rows: [tenantId, drawId, locked]
    return repo.findDueToClose(null, nowEpoch, limit).stream().map(this::mapDueToCloseRow).toList();
  }

  private DueToCloseRow mapDueToCloseRow(Object[] row) {
    TenantId tenantId = TenantId.of((UUID) row[0]);
    DrawId drawId = DrawId.of((UUID) row[1]);
    Boolean locked = row[2] == null ? Boolean.FALSE : (Boolean) row[2];
    return new DueToCloseRow(tenantId, drawId, locked);
  }

  @Override
  public int bulkClose(List<DrawId> drawIds) {
    if (drawIds == null || drawIds.isEmpty()) return 0;
    UUID[] ids = drawIds.stream().map(DrawId::uuid).toArray(UUID[]::new);
    return repo.bulkClose(ids);
  }
}
