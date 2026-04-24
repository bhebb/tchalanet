package com.tchalanet.server.features.tenantadmin.config.model;

public record TenantIdentityView(
    String tenantId,
    String code,
    String name,
    String timeZone,
    String currency,
    String status,
    String type
) {}
