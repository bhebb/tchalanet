package com.tchalanet.server.features.runtime.model;

public record TenantContextView(
    String tenantId,
    String tenantCode,
    String tenantName
) {}
