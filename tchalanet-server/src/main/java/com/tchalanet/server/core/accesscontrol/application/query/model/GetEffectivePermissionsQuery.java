package com.tchalanet.server.core.accesscontrol.application.query.model;

public record GetEffectivePermissionsQuery(
    String userId,
    String tenantId
) {}
