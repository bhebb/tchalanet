package com.tchalanet.server.tenant.domain.model;

import java.util.UUID;

public record TenantUser(
    UUID id, UUID tenantId, UUID userId, String role, String autonomyLevel, boolean owner) {}
