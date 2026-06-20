package com.tchalanet.server.features.bootstrap.privateruntime.model;

public record TenantContextView(
    String tenantId,
    String tenantCode,
    String tenantName
) {}
