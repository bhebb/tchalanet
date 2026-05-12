package com.tchalanet.server.platform.identity.api.model.view;

import com.tchalanet.server.common.types.enums.TenantUserStatus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record TenantMembershipView(
    TenantId tenantId,
    UserId userId,
    RoleId roleId,
    OutletId outletId,
    TerminalId terminalId,
    TenantUserStatus status,
    boolean owner) {}
