package com.tchalanet.server.features.tenantadmin.application.query.model;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.enums.TenantUserStatus;
import java.time.Instant;

public record TenantUserDetails(
    UserId userId,
    String username,
    String displayName,
    String email,
    TenantUserStatus membershipStatus,
    AutonomyLevel autonomyLevel,
    RoleId roleId,
    Instant createdAt) {}
