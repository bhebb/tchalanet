package com.tchalanet.server.core.audit.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;

public record AuditEventQuery(TenantId tenant, int limit) {}

