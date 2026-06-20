package com.tchalanet.server.core.sellerterminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sellerterminal.api.model.PinResetReason;
import com.tchalanet.server.core.sellerterminal.api.model.ResetSellerTerminalPinView;

public record ResetSellerTerminalPinCommand(
    TenantId tenantId,
    SellerTerminalId terminalId,
    PinResetReason reason,
    UserId actorUserId
) implements Command<ResetSellerTerminalPinView> {}
