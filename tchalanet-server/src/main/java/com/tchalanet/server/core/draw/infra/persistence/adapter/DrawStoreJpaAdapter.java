package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawStorePort;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawStoreJpaAdapter implements DrawStorePort {

  private final DrawJpaRepository repo;

  @Override
  public boolean exists(TenantId tenantId, UUID drawChannelId, Instant scheduledAt) {
    return repo.existsByTenantIdAndDrawChannel_IdAndScheduledAtAndDeletedAtIsNull(
        tenantId.uuid(), drawChannelId, scheduledAt);
  }

  @Override
  public int bulkInsert(List<NewDrawRow> rows) {
    int created = 0;
    for (NewDrawRow r : rows) {
      try {
        created +=
            repo.insertIfNotExists(
                r.id().uuid(),
                r.tenantId().uuid(),
                r.drawChannelId(),
                r.scheduledAt(),
                r.cutoffSec(),
                r.status(),
                r.drawSource());
      } catch (DataIntegrityViolationException e) {
        log.warn(e.getMessage(), e);
      }
    }
    return created;
  }

  @Override
  public List<DueToCloseRow> findDueToClose(Instant now, int limit) {
    long nowEpoch = now == null ? Instant.now().getEpochSecond() : now.getEpochSecond();
    return repo.findDueToClose(nowEpoch, limit).stream()
        .map(
            p ->
                new DueToCloseRow(
                    TenantId.of((UUID) p[0]), DrawId.of((UUID) p[1]), Boolean.TRUE.equals(p[2])))
        .toList();
  }

  @Override
  public int bulkClose(List<DrawId> drawIds) {
    if (drawIds == null || drawIds.isEmpty()) return 0;
    UUID[] ids = drawIds.stream().map(DrawId::uuid).toArray(UUID[]::new);
    return repo.bulkClose(ids);
  }
}
