package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.platform.identity.api.model.TenantUserStatus;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record TenantUserRow(
    UserId id,
    String username,
    String displayName,
    String email,
    TenantUserStatus status,
    RoleId roleId,
    Instant createdAt) {}
