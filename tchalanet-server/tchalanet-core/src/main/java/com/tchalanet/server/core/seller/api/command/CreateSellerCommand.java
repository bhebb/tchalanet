package com.tchalanet.server.core.seller.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSellerCommand(
    @NotNull TenantId tenantId,
    @NotBlank String displayName,
    String code,
    UserId userId
) implements Command<SellerId> {}
