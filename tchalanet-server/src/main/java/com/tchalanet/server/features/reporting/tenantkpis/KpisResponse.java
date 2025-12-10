package com.tchalanet.server.features.reporting.tenantkpis;

import java.math.BigDecimal;
import java.util.Map;

public record KpisResponse(
    GetTenantKpisSnapshotDto snapshot
) {}
