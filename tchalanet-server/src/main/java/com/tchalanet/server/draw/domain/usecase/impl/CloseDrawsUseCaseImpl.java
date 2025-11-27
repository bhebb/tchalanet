package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawStatus;
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
    if (drawRepository
        instanceof com.tchalanet.server.draw.infra.persistence.JpaDrawRepositoryAdapter) {
      var jpa =
          (com.tchalanet.server.draw.infra.persistence.JpaDrawRepositoryAdapter) drawRepository;
      List<Draw> toClose = jpa.findByStatusAndScheduledAtBefore("SCHEDULED", now).stream().toList();

      for (Draw d : toClose) {
        try {
          Draw updated = d.withStatus(DrawStatus.CLOSED);
          drawRepository.save(updated);
          var event =
              AuditEvent.of(
                  updated.tenantId(),
                  AuditActorType.SYSTEM,
                  "system",
                  AuditEntityType.DRAW,
                  updated.id().toString(),
                  AuditAction.UPDATE,
                  Map.of("reason", "auto-close").toString(),
                  null,
                  null);
          audit.log(event);
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
    closeDueDraws();
  }
}
