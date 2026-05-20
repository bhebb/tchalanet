package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.platform.audit.api.model.AuditEventView;

/** Maps the internal {@link AuditEvent} domain model to the public {@link AuditEventView}. */
public final class AuditEventMapper {

  private AuditEventMapper() {}

  public static AuditEventView toView(AuditEvent event) {
    return new AuditEventView(
        event.id(),
        event.tenantId(),
        event.occurredAt(),
        event.actorType(),
        event.actorId(),
        event.entityType(),
        event.entityId(),
        event.action(),
        event.detailsJson(),
        event.ip(),
        event.userAgent());
  }
}
