package com.tchalanet.server.platform.audit.api.model.request;

import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

public record ListAuditEventsRequest(
    TenantId tenantId,
    AuditEntityType entityType,
    String entityId,
    AuditAction action,
    String actorId,
    String ip,
    Instant from,
    Instant to,
    Pageable pageable) {}
