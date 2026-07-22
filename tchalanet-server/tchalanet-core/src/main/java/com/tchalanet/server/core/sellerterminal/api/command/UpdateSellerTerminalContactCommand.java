package com.tchalanet.server.core.sellerterminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record UpdateSellerTerminalContactCommand(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    AddressId addressId,
    UserId actorUserId)
    implements Command<Void> {}
