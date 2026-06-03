package com.tchalanet.server.platform.tenantgame.api.model.view;

import java.math.BigDecimal;

public record TenantGameAdminView(
    String gameCode,
    String catalogName,
    String category,
    String displayName,
    boolean enabled,
    boolean visibleInPos,
    int displayOrder,
    BigDecimal minStake,
    BigDecimal maxStake,
    boolean availabilityEnabled,
    String availabilityDays,
    String startLocalTime,
    String endLocalTime,
    boolean readyForSale
) {}
