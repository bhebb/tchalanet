package com.tchalanet.server.platform.accesscontrol.api.model;

import java.time.Instant;
import java.util.UUID;

public record TenantAdminGlobalAccessRow(
    UUID userId,
    String email,
    String displayName,
    String status,
    UUID tenantId,
    String tenantName,
    String tenantCode,
    Instant assignedAt) {}
