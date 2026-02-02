package com.tchalanet.server.features.tenantadmin.users.model;

import com.tchalanet.server.common.types.id.UserId;

public record AssignUserRequest(
    UserId userId,
    String roleId
) {}
