package com.tchalanet.server.platform.audit.api.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import java.util.Map;

public record LogAuditEventCommand(
    AuditEntityType entityType, String entityId, AuditAction action, Map<String, Object> details) implements Command<Object> {}
