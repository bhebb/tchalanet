package com.tchalanet.server.core.seller.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AssignSellerToOutletCommand(
    @NotNull TenantId tenantId,
    @NotNull SellerId sellerId,
    @NotNull OutletId outletId,
    @NotNull Instant startsAt
) implements Command<SellerOutletAssignmentId> {}
