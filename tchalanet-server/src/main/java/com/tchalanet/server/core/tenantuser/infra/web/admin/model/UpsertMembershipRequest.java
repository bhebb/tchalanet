package com.tchalanet.server.core.tenantuser.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;

public record UpsertMembershipRequest(
    OutletId outletId,
    TerminalId terminalId
) {}
