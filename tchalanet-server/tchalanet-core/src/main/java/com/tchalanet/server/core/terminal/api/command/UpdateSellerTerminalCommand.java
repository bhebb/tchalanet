package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.math.BigDecimal;

public record UpdateSellerTerminalCommand(
    TenantId tenantId,
    SellerTerminalId terminalId,
    String displayName,
    String firstName,
    String lastName,
    String phoneNumber,
    AddressId addressId,
    OutletId outletId,
    BigDecimal commissionRate,
    UserId actorUserId
) implements Command<Void> {}
