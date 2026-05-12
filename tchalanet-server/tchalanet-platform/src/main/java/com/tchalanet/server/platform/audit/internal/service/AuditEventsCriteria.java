package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

public record AuditEventsCriteria(
    TenantId tenantId,
    AuditEntityType entityType,
    String entityId,
    AuditAction action,
    String actorId,
    Instant from,
    Instant to,
    Pageable pageable) {}
