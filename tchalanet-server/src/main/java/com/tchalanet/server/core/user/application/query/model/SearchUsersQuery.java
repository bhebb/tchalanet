package com.tchalanet.server.core.user.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public record SearchUsersQuery(
    TenantId tenantId,
    String text,
    int page,
    int size) {}

