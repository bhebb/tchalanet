package com.tchalanet.server.platform.audit.api.model;

import com.tchalanet.server.common.types.id.TenantId;

public record AuditEventQuery(TenantId tenant, int limit) {}
