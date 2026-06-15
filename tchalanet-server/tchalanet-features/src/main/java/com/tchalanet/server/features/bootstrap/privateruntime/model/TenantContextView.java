package com.tchalanet.server.features.bootstrap;

public record TenantContextView(
    String tenantId,
    String tenantCode,
    String tenantName
) {}
