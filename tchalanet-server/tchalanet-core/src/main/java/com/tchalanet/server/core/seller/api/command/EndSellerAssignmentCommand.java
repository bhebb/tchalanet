package com.tchalanet.server.core.seller.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

public record EndSellerAssignmentCommand(
    @NotNull TenantId tenantId,
    @NotNull SellerId sellerId,
    @NotNull SellerOutletAssignmentId assignmentId
) implements Command<Void> {}
