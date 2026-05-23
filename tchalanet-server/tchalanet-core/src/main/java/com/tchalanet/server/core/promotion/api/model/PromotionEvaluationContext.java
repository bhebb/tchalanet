package com.tchalanet.server.core.promotion.api.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PromotionEvaluationContext(
    @NotNull TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId sellerId,
    @NotNull Instant saleAt,
    @NotNull ZoneId tenantZoneId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    @NotNull @Valid List<PromotionCartLine> cartLines,
    @NotNull BigDecimal paidTotal,
    @NotNull PromotionPhase phase,
    boolean offline
) {}
