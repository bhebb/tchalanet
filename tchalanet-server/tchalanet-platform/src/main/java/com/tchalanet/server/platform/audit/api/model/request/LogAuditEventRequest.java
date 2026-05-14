package com.tchalanet.server.platform.audit.api.model.request;

import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import java.util.Map;

public record LogAuditEventRequest(
    AuditEntityType entityType, String entityId, AuditAction action, Map<String, Object> details) {}
