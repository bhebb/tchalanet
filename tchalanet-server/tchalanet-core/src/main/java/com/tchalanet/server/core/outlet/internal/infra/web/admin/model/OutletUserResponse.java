package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.UserId;

public record OutletUserResponse(
    UserId userId,
    OutletId outletId,
    String displayName,
    String email,
    String username,
    String status,
    RoleId roleId) {}
