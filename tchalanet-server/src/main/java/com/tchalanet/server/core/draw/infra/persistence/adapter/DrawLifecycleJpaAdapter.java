package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.OpenableDrawRow;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawLifecycleJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawLifecycleJpaAdapter implements DrawLifecyclePort {

  private final DrawLifecycleJpaRepository repo;

  @Override
  public List<OpenableDrawRow> findOpenable(Instant now, int limit, int openHorizonHours, int openLagHours) {
    return repo.findOpenable(now, limit, openHorizonHours, openLagHours).stream()
        .map(
            p ->
                new OpenableDrawRow(
                    p.getTenantId(),
                    p.getDrawId(),
                    Boolean.TRUE.equals(p.getLocked()),
                    p.getScheduledAt(),
                    p.getCutoffSec() == null ? 0 : p.getCutoffSec()))
        .toList();
  }

  @Override
  public int bulkOpen(List<UUID> drawIds) {
    if (drawIds == null || drawIds.isEmpty()) return 0;
    return repo.bulkOpen(drawIds.toArray(new UUID[0]));
  }

  @Override
  public List<DueToCloseRow> findDueToClose(Instant now, int limit) {
    return repo.findDueToClose(now, limit).stream()
        .map(p -> new DueToCloseRow(p.getTenantId(), p.getDrawId(), Boolean.TRUE.equals(p.getLocked())))
        .toList();
  }

  @Override
  public int bulkClose(List<UUID> drawIds) {
    if (drawIds == null || drawIds.isEmpty()) return 0;
    return repo.bulkClose(drawIds.toArray(new UUID[0]));
  }
}
