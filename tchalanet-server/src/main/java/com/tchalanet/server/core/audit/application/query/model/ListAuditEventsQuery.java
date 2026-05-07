package com.tchalanet.server.core.audit.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
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
