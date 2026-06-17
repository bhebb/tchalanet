package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.math.BigDecimal;

public record CreateSellerTerminalCommand(
    TenantId tenantId,
    String terminalCode,
    String displayName,
    String firstName,
    String lastName,
    String phoneNumber,
    AddressId addressId,
    OutletId outletId,
    BigDecimal commissionRate,
    String initialPin,
    UserId actorUserId
) implements Command<SellerTerminalId> {}
