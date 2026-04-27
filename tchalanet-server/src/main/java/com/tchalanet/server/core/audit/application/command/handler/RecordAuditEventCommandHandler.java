package com.tchalanet.server.core.audit.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.AuditActorType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.audit.application.command.model.RecordAuditEventCommand;
import com.tchalanet.server.core.audit.application.port.out.AuditEventWriterPort;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordAuditEventCommandHandler implements VoidCommandHandler<RecordAuditEventCommand> {

  private final AuditEventWriterPort writerPort;
  private final TchContextResolver contextResolver;
  private final JsonUtils jsonUtils;

  @Override
  public void handle(RecordAuditEventCommand command) {
    var ctx = contextResolver.currentOrNull();
    var ctxOpt = Optional.ofNullable(ctx);

    var userOpt = ctxOpt.map(TchRequestContext::userId);
    var createdBy = userOpt.orElse(null);
    var actorId = createdBy;
    var actorType = userOpt.isPresent() ? AuditActorType.USER : AuditActorType.SYSTEM;
    var ip = ctxOpt.map(TchRequestContext::clientIp).orElse(null);
    var userAgent = ctxOpt.map(TchRequestContext::userAgent).orElse(null);

    var details = Optional.ofNullable(command.details()).map(HashMap::new).orElseGet(HashMap::new);

    ctxOpt
        .map(TchRequestContext::requestId)
        .ifPresent(requestId -> details.putIfAbsent("requestId", requestId));

    var json =
        Optional.of(details)
            .map(jsonUtils::toJson)
            .orElse("{}");

    UUID createdByUuid = createdBy != null ? createdBy.uuid() : null;
    UUID actorIdUuid = actorId != null ? actorId.uuid() : null;

    var event =
        new AuditEvent(
            null,
            TenantId.nullableOf(ctx != null ? ctx.tenantUuid() : null),
            Instant.now(),
            createdByUuid,
            actorType,
            actorIdUuid,
            command.entityType(),
            UUID.fromString(command.entityId()),
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
      log.error("Failed to deserialize UUID from String", ex);
      return null;
    }
  }
}
