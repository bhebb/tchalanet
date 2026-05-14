package com.tchalanet.server.platform.audit.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;

public record AuditEventRequest(TenantId tenant, int limit) {}
