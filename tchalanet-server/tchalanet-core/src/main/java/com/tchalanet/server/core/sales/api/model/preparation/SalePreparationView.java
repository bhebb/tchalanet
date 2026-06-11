package com.tchalanet.server.core.sales.api.model.preparation;

import com.tchalanet.server.common.web.api.ApiNotice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SalePreparationView(
    UUID preparationId,
    SalePreparationStatus status,
    Instant expiresAt,
    String currency,
    BigDecimal totalAmount,
    List<SalePreparationLineView> lines,
    List<SalePreparationPromotionLineView> promotionLines,
    List<ApiNotice> notices
) {}
