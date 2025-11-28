package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.audit.application.port.in.LogAuditEventCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawSource;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.domain.usecase.AdminOverrideResultUseCase;
import com.tchalanet.server.draw.web.dto.OverrideResultRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AdminOverrideResultUseCaseImpl implements AdminOverrideResultUseCase {

  private final DrawRepository drawRepository;
  private final LogAuditEventCommandHandler audit;

  @Override
  public void overrideResult(UUID tenantId, UUID drawId, OverrideResultRequest req, UUID adminId) {
    log.info("Admin {} is overriding result for draw {}", adminId, drawId);

    // 1. Find the entity
    Draw draw =
        drawRepository
            .findById(drawId)
            .orElseThrow(() -> new IllegalArgumentException("Draw not found with id: " + drawId));

    // 2. Authorize the action
    if (!draw.tenantId().equals(tenantId)) {
      throw new SecurityException("Tenant mismatch for draw " + drawId);
    }

    // 3. Prepare the data for the domain method
    Map<String, Object> payload = new HashMap<>();
    payload.put("numbers", req.numbers());
    if (req.extra() != null) {
      payload.putAll(req.extra());
    }

    // 4. Call the rich domain model method
    // The entity itself is responsible for validation and state transition.
    Draw updatedDraw = draw.applyResult(payload, DrawSource.MANUAL, adminId);

    // 5. Save the new state
    drawRepository.save(updatedDraw);

    // 6. Audit the manual override (build lightweight command; UseCase will enrich via factory)
    var details =
        Map.<String, Object>of(
            "admin", adminId.toString(),
            "action", "override_result",
            "new_numbers", req.numbers());
    audit.handle(
        new LogAuditEventCommand(
            com.tchalanet.server.audit.domain.model.AuditEntityType.DRAW,
            drawId.toString(),
            com.tchalanet.server.audit.domain.model.AuditAction.UPDATE,
            details));

    log.info("Successfully overrode result for draw {}", drawId);
  }
}
