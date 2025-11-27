package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.domain.model.DrawStatus;
import com.tchalanet.server.draw.domain.ports.DrawRepository;
import com.tchalanet.server.draw.domain.usecase.AdminUpdateDrawUseCase;
import com.tchalanet.server.draw.web.dto.UpdateDrawRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AdminUpdateDrawUseCaseImpl implements AdminUpdateDrawUseCase {

  private final DrawRepository drawRepository;
  private final LogAuditEventUseCase audit;

  @Override
  public Draw updateDraw(UUID tenantId, UUID drawId, UpdateDrawRequest req, UUID adminId) {
    Optional<Draw> opt = drawRepository.findById(drawId);
    var d = opt.orElseThrow(() -> new IllegalArgumentException("Draw not found"));

    // basic tenant check
    if (!d.tenantId().equals(tenantId)) throw new IllegalArgumentException("Tenant mismatch");

    // apply changes
    boolean changed = false;
    if (req.getScheduledAt() != null) {
      d = d.withScheduledAt(req.getScheduledAt().toInstant());
      changed = true;
    }
    if (req.getCutoffSec() != null) {
      d = d.withCutoffSec(req.getCutoffSec());
      changed = true;
    }
    if (req.getStatus() != null) {
      d = d.withStatus(DrawStatus.valueOf(req.getStatus()));
      changed = true;
    }
    if (req.getResultPayload() != null) {
      d = d.withResultPayload(req.getResultPayload().toString());
      changed = true;
    }

    // mark manual edit (distinct from manual result)
    d = d.withSystemGenerated(false);
    if (req.getLocked() != null) d = d.withLocked(req.getLocked());
    else d = d.withLocked(true);

    d = d.withUpdatedBy(adminId);

    // save
    var saved = drawRepository.save(d);

    // audit
    var ev =
        AuditEvent.of(
            saved.tenantId(),
            AuditActorType.USER,
            adminId == null ? "admin" : adminId.toString(),
            AuditEntityType.DRAW,
            saved.id().toString(),
            AuditAction.UPDATE,
            Map.of("by", "admin").toString(),
            null,
            null);
    audit.log(ev);

    return saved;
  }
}
