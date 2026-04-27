package com.tchalanet.server.features.tenantadmin.users.model;

import com.tchalanet.server.common.types.enums.TenantUserStatus;
import java.time.Instant;
import java.util.Optional;

public record TenantUserFilter(
    Optional<String> q,
    Optional<TenantUserStatus> status,
    Optional<Instant> createdAfter
) {}
