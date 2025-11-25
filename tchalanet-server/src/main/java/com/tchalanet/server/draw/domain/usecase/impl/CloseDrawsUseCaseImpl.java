package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.audit.domain.model.AuditAction;
import com.tchalanet.server.common.audit.domain.model.AuditEntityType;
import com.tchalanet.server.common.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.domain.usecase.CloseDueDrawsUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloseDrawsUseCaseImpl implements CloseDueDrawsUseCase {

  private final DrawRepository drawRepository;
  private final LogAuditEventUseCase audit;

  @Transactional
  public void closeDueDraws() {
    Instant now = Instant.now();
    // We rely on the adapter implementation to expose a helper; fallback to repository call via
    // adapter
    if (drawRepository
        instanceof com.tchalanet.server.draw.infra.persistence.JpaDrawRepositoryAdapter) {
      var jpa =
          (com.tchalanet.server.draw.infra.persistence.JpaDrawRepositoryAdapter) drawRepository;
      // find scheduled draws with scheduled_at <= now
      List<Draw> toClose =
          jpa.findByStatusAndScheduledAtBefore("SCHEDULED", now).stream()
              .map(d -> (Draw) d)
              .toList();

      for (Draw d : toClose) {
        try {
          var updated =
              new Draw(
                  d.id(),
                  d.tenantId(),
                  d.drawChannelId(),
                  d.gameCode(),
                  d.scheduledAt(),
                  d.cutoffSec(),
                  "CLOSED",
                  d.resultPayload(),
                  d.drawSource(),
                  d.systemGenerated(),
                  d.locked(),
                  d.createdBy(),
                  d.updatedBy());
          drawRepository.save(updated);
          audit.log(
              AuditEntityType.DRAW,
              d.id().toString(),
              AuditAction.UPDATE,
              Map.of("reason", "auto-close"));
        } catch (Exception e) {
          log.warn("Failed to close draw {}: {}", d.id(), e.getMessage());
        }
      }
    } else {
      log.warn("CloseDrawsUseCaseImpl: drawRepository not JPA adapter, skipping auto-close");
    }
  }

  @Override
  public void execute() {
    // Delegate to existing method to keep behavior
    closeDueDraws();
  }
}
