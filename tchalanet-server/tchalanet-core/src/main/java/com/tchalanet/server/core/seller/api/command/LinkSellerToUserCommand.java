package com.tchalanet.server.core.seller.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record LinkSellerToUserCommand(
    @NotNull TenantId tenantId,
    @NotNull SellerId sellerId,
    @NotNull UserId userId
) implements Command<Void> {}
