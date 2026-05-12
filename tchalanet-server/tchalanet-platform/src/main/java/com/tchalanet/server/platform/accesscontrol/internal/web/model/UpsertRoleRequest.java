package com.tchalanet.server.platform.accesscontrol.internal.web.model;

import java.util.UUID;

public record UpsertRoleRequest(
    UUID id,
    String code,
    String name,
    String description,
    UUID tenantId,
    UUID parentRoleId,
    boolean system) {}
