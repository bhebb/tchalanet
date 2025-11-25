package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.audit.domain.model.AuditAction;
import com.tchalanet.server.common.audit.domain.model.AuditEntityType;
import com.tchalanet.server.common.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.domain.usecase.AdminOverrideResultUseCase;
import com.tchalanet.server.draw.web.dto.OverrideResultRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    Optional<com.tchalanet.server.draw.domain.model.Draw> opt = drawRepository.findById(drawId);
    if (opt.isEmpty()) throw new IllegalArgumentException("Draw not found");
    var d = opt.get();
    if (!d.tenantId().equals(tenantId)) throw new IllegalArgumentException("Tenant mismatch");

    // Build payload
    Map<String, Object> payload = new HashMap<>();
    payload.put("numbers", req.numbers());
    if (req.extra() != null) payload.putAll(req.extra());

    // Mark manual result and lock
    var updated =
        new com.tchalanet.server.draw.domain.model.Draw(
            d.id(),
            d.tenantId(),
            d.drawChannelId(),
            d.gameCode(),
            d.scheduledAt(),
            d.cutoffSec(),
            "RESULTED",
            payload,
            "MANUAL_RESULT",
            Boolean.FALSE,
            Boolean.TRUE,
            d.createdBy(),
            adminId);

    drawRepository.save(updated);

    // audit the manual override
    audit.log(
        AuditEntityType.DRAW,
        drawId.toString(),
        AuditAction.UPDATE,
        Map.of("admin", adminId.toString(), "action", "override_result"));
  }
}
