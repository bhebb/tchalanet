package com.tchalanet.server.features.tenantadmin.users.model;

import com.tchalanet.server.common.types.enums.TchRole;

public record SetUserRoleRequest(
    TchRole role
) {}
