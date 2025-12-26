package com.tchalanet.server.core.audit.application.command.model;

import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import java.util.Map;

public record LogAuditEventCommand(
    AuditEntityType entityType, String entityId, AuditAction action, Map<String, Object> details) {}
