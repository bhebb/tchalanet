package com.tchalanet.server.core.audit.domain.service;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditActorType;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
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

  private final TchContextResolver contextResolver;
  private final JsonUtils jsonUtils;

  public AuditEvent build(
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      Map<String, Object> details) {

    TchRequestContext ctx = contextResolver.currentOrNull();

    UUID tenantUuid = (ctx != null) ? ctx.tenantUuid() : null;
    UUID userUuid = (ctx != null) ? ctx.userUuid() : null;

    AuditActorType actorType = (userUuid != null) ? AuditActorType.USER : AuditActorType.SYSTEM;
    String ip = (ctx != null) ? ctx.clientIp() : null;
    String userAgent = (ctx != null) ? ctx.userAgent() : null;

    UUID entityUuid = Optional.ofNullable(entityId).map(UUID::fromString).orElse(null);

    String detailsJson =
        Optional.ofNullable(details)
            .map(
                d -> {
                  try {
                    return jsonUtils.toJson(d);
                  } catch (Exception ex) {
                    log.error("Failed to serialize audit details", ex);
                    return "{}";
                  }
                })
            .orElse("{}");

    return new AuditEvent(
        null,
        TenantId.nullableOf(tenantUuid), // ✅ safe for platform/system
        Instant.now(),
        userUuid, // createdBy (must be null if SYSTEM)
        actorType,
        userUuid, // actorId (required if USER)
        entityType,
        entityUuid,
        action,
        detailsJson,
        ip,
        userAgent);
  }
}
