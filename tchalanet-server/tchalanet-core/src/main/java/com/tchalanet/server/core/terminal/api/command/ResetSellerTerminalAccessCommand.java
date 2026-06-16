package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record ResetSellerTerminalAccessCommand(
    TenantId tenantId,
    SellerTerminalId terminalId,
    String newCredential,
    UserId actorUserId
) implements Command<Void> {}
