package com.tchalanet.server.core.sellerterminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

public record ChangeSellerTerminalPinCommand(
    TenantId tenantId,
    SellerTerminalId terminalId,
    String newPin
) implements Command<Void> {}
