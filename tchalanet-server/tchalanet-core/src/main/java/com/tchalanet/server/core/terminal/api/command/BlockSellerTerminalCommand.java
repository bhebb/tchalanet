package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record BlockSellerTerminalCommand(
    TenantId tenantId,
    SellerTerminalId terminalId,
    String reason,
    UserId actorUserId
) implements Command<Void> {}
