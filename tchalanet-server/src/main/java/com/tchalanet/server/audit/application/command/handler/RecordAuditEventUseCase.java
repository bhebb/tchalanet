package com.tchalanet.server.audit.application.command.handler;

import com.tchalanet.server.audit.application.command.model.RecordAuditEventCommand;
import com.tchalanet.server.audit.application.port.in.RecordAuditEventCommandHandler;
import com.tchalanet.server.audit.application.port.out.AuditEventWriterPort;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.common.stereotype.UseCase;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordAuditEventUseCase implements RecordAuditEventCommandHandler {

  private final AuditEventWriterPort writerPort;
  private final TchRequestContextHolder ctxHolder;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  @Override
  public void handle(RecordAuditEventCommand command) {
    var ctx = ctxHolder.get();

    UUID tenantId = null;
    UUID createdBy = null;
    String actorId = "system";
    var actorType = AuditActorType.SYSTEM;
    String ip = null;
    String userAgent = null;

    if (ctx != null) {

      if (ctx.userId() != null) {
        createdBy = safeUuid(ctx.userId());
        actorId = ctx.userId();
        actorType = AuditActorType.USER;
      }
      ip = ctx.clientIp();
      userAgent = ctx.userId(); // à ajouter dans ton TchRequestContext si pas déjà
    }

    Map<String, Object> details =
        command.details() != null ? new HashMap<>(command.details()) : new HashMap<>();

    if (ctx != null && ctx.requestId() != null) {
      details.putIfAbsent("requestId", ctx.requestId());
    }

    String json;
    try {
      json = objectMapper.writeValueAsString(details);
    } catch (Exception ex) {
      log.error("Failed to serialize audit details", ex);
      json = "{}";
    }

    AuditEvent event =
        new AuditEvent(
            null,
            tenantId,
            Instant.now(),
            createdBy,
            actorType,
            actorId,
            command.entityType(),
            command.entityId(),
            command.action(),
            json,
            ip,
            userAgent);

    writerPort.save(event);
  }

  private UUID safeUuid(String s) {
    if (s == null) return null;
    try {
      return java.util.UUID.fromString(s);
    } catch (Exception ex) {
      return null;
    }
  }
}
