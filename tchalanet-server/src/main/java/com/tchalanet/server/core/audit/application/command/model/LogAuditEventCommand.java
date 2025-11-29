package com.tchalanet.server.core.audit.application.command.model;

import com.tchalanet.server.core.audit.domain.model.AuditAction;
import com.tchalanet.server.core.audit.domain.model.AuditEntityType;
import java.util.Map;

public record LogAuditEventCommand(
    AuditEntityType entityType, String entityId, AuditAction action, Map<String, Object> details) {}
