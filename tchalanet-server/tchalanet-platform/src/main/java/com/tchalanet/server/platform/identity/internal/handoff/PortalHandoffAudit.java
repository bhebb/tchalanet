package com.tchalanet.server.platform.identity.internal.handoff;

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
class PortalHandoffAudit {
  private final AuditApi auditApi;

  void log(String event, UUID handoffId, UUID subjectUserId, PortalTarget targetPortal) {
    var details = new LinkedHashMap<String, Object>();
    details.put("event", event);
    if (subjectUserId != null) details.put("subjectUserId", subjectUserId);
    if (targetPortal != null) details.put("targetPortal", targetPortal.name());
    try {
      auditApi.logAuditEvent(new LogAuditEventRequest(
          AuditEntityType.SYSTEM,
          handoffId == null ? "unknown" : handoffId.toString(),
          AuditAction.OTHER,
          Map.copyOf(details),
          null));
    } catch (Exception ex) {
      log.warn("portal handoff audit failed event={} handoffId={}", event, handoffId, ex);
    }
  }
}
