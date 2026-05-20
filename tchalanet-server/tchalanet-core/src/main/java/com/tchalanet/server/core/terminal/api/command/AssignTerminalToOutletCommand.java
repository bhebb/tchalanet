package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record AssignTerminalToOutletCommand(
    TenantId tenantId, TerminalId terminalId, OutletId outletId, UserId actorUserId)
    implements Command<Void> {}
