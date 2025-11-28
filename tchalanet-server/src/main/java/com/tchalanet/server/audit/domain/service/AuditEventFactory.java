package com.tchalanet.server.audit.domain.service;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Build and enrich AuditEvent from minimal inputs (entity/action/details). */
@Component
@RequiredArgsConstructor
public class AuditEventFactory {

  private final TchRequestContextHolder ctxHolder;

  public AuditEvent build(
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      Map<String, Object> details) {
    var ctx = ctxHolder.get();

    UUID tenantId = null;
    UUID createdBy = null;
    String actorId = "system";
    var actorType = com.tchalanet.server.audit.domain.model.AuditActorType.SYSTEM;
    String ip = null;
    String userAgent = null;

    if (ctx != null) {
      if (ctx.tenantUuid() != null) {
        tenantId = ctx.tenantUuid();
      }
      if (ctx.userUuid() != null) {
        createdBy = ctx.userUuid();
        actorId = ctx.userUuid().toString();
        actorType = com.tchalanet.server.audit.domain.model.AuditActorType.USER;
      }
      ip = ctx.clientIp();
      userAgent = ctx.userAgent();
    }

    String detailsJson = details == null ? "{}" : details.toString();

    return new AuditEvent(
        null,
        tenantId,
        java.time.Instant.now(),
        createdBy,
        actorType,
        actorId,
        entityType,
        entityId,
        action,
        detailsJson,
        ip,
        userAgent);
  }
}
