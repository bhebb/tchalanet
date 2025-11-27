package com.tchalanet.server.accesscontrol.infra.web.dto;

import java.util.UUID;

public record UpsertRoleRequest(
    UUID id,
    String code,
    String name,
    String description,
    UUID tenantId,
    UUID parentRoleId,
    boolean system) {}
