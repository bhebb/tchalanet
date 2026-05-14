package com.tchalanet.server.platform.audit.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.audit.internal.service.AuditEvent;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

public record ListAuditEventsQuery(
    TenantId tenantId,
    AuditEntityType entityType,
    String entityId,
    AuditAction action,
    String actorId,
    Instant from,
    Instant to,
    Pageable pageable)
    implements Query<TchPage<AuditEvent>> {}
