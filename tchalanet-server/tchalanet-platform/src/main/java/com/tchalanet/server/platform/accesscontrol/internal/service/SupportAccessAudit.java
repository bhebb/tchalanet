package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.SupportAccessSessionJpaEntity;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class SupportAccessAudit {
  private final AuditApi auditApi;

  void log(String event, SupportAccessSessionJpaEntity session) {
    if (session == null) {
      log(event, null, null, null);
      return;
    }
    log(event, session.getId(), session.getSuperAdminUserId(), session.getTenantId());
  }

  void log(String event, UUID sessionId, UUID superAdminUserId, UUID tenantId) {
    var details = new LinkedHashMap<String, Object>();
    details.put("event", event);
    if (superAdminUserId != null) details.put("superAdminUserId", superAdminUserId);
    if (tenantId != null) details.put("tenantId", tenantId);
    try {
      auditApi.logAuditEvent(
          new LogAuditEventRequest(
              AuditEntityType.SYSTEM,
              sessionId == null ? "unknown" : sessionId.toString(),
              AuditAction.OTHER,
              Map.copyOf(details),
              tenantId));
    } catch (Exception ex) {
      log.warn("support access audit failed event={} sessionId={}", event, sessionId, ex);
    }
  }
}
