package com.tchalanet.server.core.sellerterminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.math.BigDecimal;

public record SetSellerTerminalCommissionRateCommand(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    BigDecimal commissionRate,
    UserId actorUserId
) implements Command<Void> {}
