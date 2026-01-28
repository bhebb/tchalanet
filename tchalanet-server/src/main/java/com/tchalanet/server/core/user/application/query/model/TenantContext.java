package com.tchalanet.server.core.user.application.query.model;

import java.util.UUID;

public record TenantContext(
    UUID tenantId,
    String tenantCode,
    String timeZone,
    String currency) {}
