package com.tchalanet.server.platform.identity.internal.model;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.TenantUserStatus;
import java.time.Instant;

public record TenantUserRow(
    UserId id,
    String username,
    String displayName,
    String email,
    TenantUserStatus status,
    Instant createdAt) {}
