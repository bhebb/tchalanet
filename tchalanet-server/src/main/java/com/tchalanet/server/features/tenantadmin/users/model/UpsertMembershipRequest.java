package com.tchalanet.server.features.tenantadmin.users.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;

public record UpsertMembershipRequest(
    OutletId outletId,
    TerminalId terminalId
) {}
