package com.tchalanet.server.draw.application;

import com.tchalanet.server.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.common.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.draw.application.ports.in.ApplyDrawResultUseCase;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOverrideResultUseCaseImpl implements AdminOverrideResultUseCase {

  private final DrawRepository drawRepository; // Still needed for initial find
  private final LogAuditEventUseCase audit;
  private final ApplyDrawResultUseCase applyDrawResultUseCase; // New: Inject ApplyDrawResultUseCase

  @Override
  @Transactional
  @RequiresPermission("draw.override_result")
  public void overrideResult(UUID tenantId, UUID drawId, OverrideResultRequest req, UUID adminId) {
    log.info("Admin {} is overriding result for draw {}", adminId, drawId);

    // 1. Find the entity (only for tenant check, actual update is via ApplyDrawResultUseCase)
    Draw draw =
        drawRepository
            .findById(drawId)
            .orElseThrow(() -> new IllegalArgumentException("Draw not found with id: " + drawId));

    // 2. Authorize the action (tenant mismatch check)
    if (!draw.tenantId().equals(tenantId)) {
      throw new SecurityException("Tenant mismatch for draw " + drawId);
    }

    // 3. Prepare the data for the ApplyDrawResultUseCase
    Map<String, Object> payload = new HashMap<>();
    payload.put("numbers", req.numbers());
    if (req.extra() != null) {
      payload.putAll(req.extra());
    }

    // 4. Call the generic ApplyDrawResultUseCase by building a command
    var command =
        new ApplyDrawResultUseCase.ApplyDrawResultCommand(
            drawId, tenantId, payload, DrawSource.MANUAL, adminId);
    applyDrawResultUseCase.applyResult(command);

    // 5. Audit the manual override (audit is already handled by ApplyDrawResultService, but this is
    // specific to override)
    var ev =
        AuditEvent.of(
            tenantId,
            AuditActorType.USER,
            adminId == null ? "admin" : adminId.toString(),
            AuditEntityType.DRAW,
            drawId.toString(),
            AuditAction.UPDATE,
            Map.of("reason", "admin_override").toString(),
            null,
            null);
    audit.log(ev);

    log.info("Successfully overrode result for draw {}", drawId);
  }
}
