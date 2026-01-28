package com.tchalanet.server.core.user.infra.web.model;

import java.util.UUID;

public record TenantContextResponse(
    UUID tenantId,
    String tenantCode,
    String timeZone,
    String currency) {}
