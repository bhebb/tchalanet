package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.common.domain.UseCase;
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
  private final LogAuditEventUseCase audit;

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

    // 6. Audit the manual override
    AuditEvent ev =
        AuditEvent.of(
            updatedDraw.tenantId(),
            com.tchalanet.server.audit.domain.model.AuditActorType.USER,
            adminId.toString(),
            com.tchalanet.server.audit.domain.model.AuditEntityType.DRAW,
            drawId.toString(),
            com.tchalanet.server.audit.domain.model.AuditAction.UPDATE,
            Map.of(
                    "admin",
                    adminId.toString(),
                    "action",
                    "override_result",
                    "new_numbers",
                    req.numbers())
                .toString(),
            null,
            null);
    audit.log(ev);

    log.info("Successfully overrode result for draw {}", drawId);
  }
}
