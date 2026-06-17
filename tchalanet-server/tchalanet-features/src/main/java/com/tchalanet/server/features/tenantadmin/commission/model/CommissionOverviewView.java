package com.tchalanet.server.features.tenantadmin.commission.model;

import java.math.BigDecimal;

public record CommissionOverviewView(
    BigDecimal tenantDefaultRate,
    long totalSellerTerminals,
    long countAtDefaultRate,
    long countWithCustomRate,
    BigDecimal minRate,
    BigDecimal maxRate
) {}
