package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.audit.application.port.in.LogAuditEventCommandHandler;
import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.draw.application.ports.in.CloseDueDrawsUseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawStatus;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
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
  private final LogAuditEventCommandHandler audit;

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
          var details = Map.<String, Object>of("reason", "auto-close");
          audit.handle(
              new LogAuditEventCommand(
                  AuditEntityType.DRAW, updated.id().toString(), AuditAction.UPDATE, details));
        } catch (Exception e) {
          log.warn("Failed to close draw {}: {}", d.id(), e.getMessage());
        }
      }
    } else {
      log.warn("CloseDrawsUseCaseImpl: drawRepository not JPA adapter, skipping auto-close");
    }
  }

  public void execute() {
    closeDueDraws();
  }
}
