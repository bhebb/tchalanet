package com.tchalanet.server.core.sellerterminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record UnblockSellerTerminalCommand(
    TenantId tenantId,
    SellerTerminalId terminalId,
    UserId actorUserId
) implements Command<Void> {}
