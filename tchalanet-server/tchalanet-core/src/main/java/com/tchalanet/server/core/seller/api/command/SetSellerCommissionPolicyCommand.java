package com.tchalanet.server.core.seller.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerCommissionPolicyId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.seller.api.model.SellerCommissionBase;
import com.tchalanet.server.core.seller.api.model.SellerCommissionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record SetSellerCommissionPolicyCommand(
    @NotNull TenantId tenantId,
    @NotNull SellerId sellerId,
    @NotNull SellerCommissionType type,
    @NotNull SellerCommissionBase base,
    BigDecimal ratePercent,
    BigDecimal fixedAmount,
    String currency,
    @NotNull Instant startsAt
) implements Command<SellerCommissionPolicyId> {}
