package com.tchalanet.server.features.tenantadmin.users.model;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.TenantUserStatus;
import java.time.Instant;

/**
 * Web-layer row model for tenant user list responses (feature tenantadmin.users).
 * Mirrors the application/query read model but lives in the web package so controllers
 * and mappers can depend on a stable web contract.
 */
public record TenantUserRow(
    UserId userId,
    String username,
    String displayName,
    String email,
    TenantUserStatus membershipStatus,
    AutonomyLevel autonomyLevel,
    RoleId roleId,
    Instant createdAt) {}
