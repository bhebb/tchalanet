package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.audit.domain.model.AuditAction;
import com.tchalanet.server.common.audit.domain.model.AuditEntityType;
import com.tchalanet.server.common.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort.DrawExternalQuery;
import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort.ExternalDrawResult;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.domain.usecase.InvalidateDrawCacheUseCase;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SettleDrawsUseCaseImpl
    implements com.tchalanet.server.draw.domain.usecase.SettleDrawsUseCase {

  private final DrawRepository drawRepository;
  private final ExternalDrawResultPort external; // provider router
  private final InvalidateDrawCacheUseCase invalidator;
  private final LogAuditEventUseCase auditLog;

  @Override
  @Transactional
  public void execute() {
    // 1. find closed draws scheduled before now
    Instant now = Instant.now();

    if (!(drawRepository
        instanceof com.tchalanet.server.draw.infra.persistence.JpaDrawRepositoryAdapter)) {
      log.warn("DrawRepository is not the expected JPA adapter; settlement skipped");
      return;
    }

    var jpaAdapter =
        (com.tchalanet.server.draw.infra.persistence.JpaDrawRepositoryAdapter) drawRepository;
    List<Draw> toSettle = jpaAdapter.findClosedScheduledBefore(now);
    log.info("Found {} draws to settle", toSettle.size());

    for (Draw d : toSettle) {
      try {
        DrawExternalQuery q =
            new DrawExternalQuery(
                d.tenantId(),
                d.gameCode(),
                Instant.ofEpochMilli(d.scheduledAt().toEpochMilli())
                    .atOffset(java.time.ZoneOffset.UTC)
                    .toLocalDate());
        java.util.Optional<ExternalDrawResult> maybe = external.fetchResult(q);
        if (maybe.isPresent()) {
          ExternalDrawResult res = maybe.get();
          Map<String, Object> payload = new HashMap<>();
          payload.put("numbers", res.numbers());
          payload.put(
              "provider",
              res.extra() != null ? res.extra().getOrDefault("provider", "external") : "external");
          payload.put("fetchedAt", Instant.now().toString());

          // Try to apply result only if editable (adapter will skip locked/manual-result rows)
          boolean applied =
              jpaAdapter.applyResultIfEditable(d.id(), (String) payload.get("provider"), payload);
          if (applied) {
            // invalidate cache and audit
            invalidator.invalidateTenant(d.tenantId());
            auditLog.log(
                AuditEntityType.DRAW,
                d.id().toString(),
                AuditAction.CREATE,
                Map.of("action", "settled", "provider", payload.get("provider")));
          } else {
            log.info(
                "Skipped applying external result for draw {} because it's locked or manual",
                d.id());
            auditLog.log(
                AuditEntityType.DRAW,
                d.id().toString(),
                AuditAction.UPDATE,
                Map.of("action", "settle_skipped", "reason", "locked_or_manual"));
          }
        } else {
          log.info("No external result for draw {} / {}", d.id(), d.gameCode());
        }
      } catch (Exception e) {
        log.error("Failed to settle draw {}", d.id(), e);
      }
    }
  }
}
