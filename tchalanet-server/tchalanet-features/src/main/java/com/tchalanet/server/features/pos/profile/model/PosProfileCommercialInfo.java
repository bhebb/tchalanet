package com.tchalanet.server.features.pos.profile.model;

import java.math.BigDecimal;

public record PosProfileCommercialInfo(
    String tenantId,
    String tenantCode,
    String tenantDisplayName,
    String currency,
    BigDecimal commissionRate) {}
