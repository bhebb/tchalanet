package com.tchalanet.server.platform.identity.api.model.request;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record AssignUserToTenantRequest(
    TenantId tenantId,
    UserId userId,
    RoleId roleId,
    OutletId outletId,
    TerminalId terminalId,
    boolean owner) {}
