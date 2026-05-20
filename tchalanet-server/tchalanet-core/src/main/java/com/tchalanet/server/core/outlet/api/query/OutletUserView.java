package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.UserId;

public record OutletUserView(
    UserId userId,
    OutletId outletId,
    String displayName,
    String email,
    String username,
    String status,
    RoleId roleId) {}
