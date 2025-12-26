package com.tchalanet.server.core.audit.domain.service;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditActorType;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Build and enrich AuditEvent from minimal inputs (entity/action/details). */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventFactory {

  private final TchRequestContextHolder ctxHolder;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  public AuditEvent build(
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      Map<String, Object> details) {
    var ctxOpt = Optional.ofNullable(ctxHolder.get());

    var tenantId = ctxOpt.map(TchRequestContext::tenantUuid).orElse(null);
    var userOpt = ctxOpt.map(TchRequestContext::userUuid);
    var createdBy = userOpt.orElse(null);
    var actorId = userOpt.orElse(null);
    var actorType = userOpt.isPresent() ? AuditActorType.USER : AuditActorType.SYSTEM;
    var ip = ctxOpt.map(TchRequestContext::clientIp).orElse(null);
    var userAgent = ctxOpt.map(TchRequestContext::userAgent).orElse(null);

    var entityUuid = Optional.ofNullable(entityId).map(UUID::fromString).orElse(null);
    var detailsJson =
        Optional.ofNullable(details)
            .map(
                d -> {
                  try {
                    return objectMapper.writeValueAsString(d);
                  } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    return "{}";
                  }
                })
            .orElse("{}");

    return new AuditEvent(
        null,
        TenantId.of(tenantId),
        Instant.now(),
        createdBy,
        actorType,
        actorId,
        entityType,
        entityUuid,
        action,
        detailsJson,
        ip,
        userAgent);
  }
}
