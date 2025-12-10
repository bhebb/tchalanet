package com.tchalanet.server.core.accesscontrol.application.query.model;

public record CheckUserPermissionsQuery(
    String userId,
    String tenantId,
    String resource,
    String action
) {}
