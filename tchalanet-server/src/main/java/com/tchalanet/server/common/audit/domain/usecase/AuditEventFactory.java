package com.tchalanet.server.common.audit.domain.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.audit.domain.model.*;
import com.tchalanet.server.common.context.RequestContextHolder;
import com.tchalanet.server.common.context.TchRequestContext;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventFactory {

  private final RequestContextHolder ctxHolder;
  private final ObjectMapper objectMapper;

  public AuditEvent build(
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      Map<String, Object> details) {
    TchRequestContext ctx = ctxHolder.get();

    UUID tenantId = ctx != null && ctx.tenantId() != null ? safeUuid(ctx.tenantId()) : null;

    String userId = ctx != null ? ctx.userId() : null;
    String ip = ctx != null ? ctx.clientIp() : null; // adapte à ton TchRequestContext
    String requestId = ctx != null ? ctx.requestId() : null;

    AuditActorType actorType =
        (ctx == null || ctx.userId() == null) ? AuditActorType.SYSTEM : AuditActorType.USER;

    // enrichit les details avec reqId éventuellement
    if (details != null && requestId != null) {
      details.putIfAbsent("requestId", requestId);
    }

    String json;
    try {
      json = objectMapper.writeValueAsString(details != null ? details : Map.of());
    } catch (JsonProcessingException e) {
      json = "{}";
    }

    UUID createdBy = safeUuid(userId);

    return new AuditEvent(
        null,
        tenantId,
        Instant.now(),
        createdBy,
        actorType,
        userId != null ? userId : "system",
        entityType,
        entityId,
        action,
        json,
        ip,
        null // userAgent => tu peux l'ajouter au TchRequestContext si tu veux
        );
  }

  private UUID safeUuid(String s) {
    if (s == null) return null;
    try {
      return UUID.fromString(s);
    } catch (Exception ex) {
      return null;
    }
  }
}
