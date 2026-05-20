package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record ValidateTerminalForOperationQuery(
    TenantId tenantId,
    TerminalId terminalId,
    OutletId outletId,
    UserId actorUserId,
    TerminalOperation operation
) implements Query<ValidatedTerminalOperationView> {}
