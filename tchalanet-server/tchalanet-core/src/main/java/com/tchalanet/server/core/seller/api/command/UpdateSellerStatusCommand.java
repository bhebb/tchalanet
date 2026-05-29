package com.tchalanet.server.core.seller.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.seller.api.model.SellerStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerStatusCommand(
    @NotNull TenantId tenantId,
    @NotNull SellerId sellerId,
    @NotNull SellerStatus status
) implements Command<Void> {}
