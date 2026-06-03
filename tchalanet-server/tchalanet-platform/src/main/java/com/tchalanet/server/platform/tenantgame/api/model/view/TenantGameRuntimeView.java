package com.tchalanet.server.platform.tenantgame.api.model.view;

import java.math.BigDecimal;

/**
 * Safe runtime view for POS/sales/bootstrap.
 * No internal IDs, no deleted flags, no admin-only config.
 */
public record TenantGameRuntimeView(
    String gameCode,
    String label,
    String category,
    boolean saleEnabled,
    boolean visibleInPos,
    int displayOrder,
    BigDecimal minStake,
    BigDecimal maxStake,
    boolean availabilityEnabled,
    String availabilityDays,
    String startLocalTime,
    String endLocalTime
) {}
