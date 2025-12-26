package com.tchalanet.server.features.stats.tenant_dashboard.application;

import com.tchalanet.server.features.stats.tenant_dashboard.dto.TenantDashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import com.tchalanet.server.common.types.id.TenantId;

@Service
@RequiredArgsConstructor
public class TenantDashboardStatsUseCase {

    private final TenantDashboardFromAggregatesService aggregatesService;

    public TenantDashboardStatsResponse getStats(
        TenantId tenantId,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        var query = new TenantDashboardStatsQuery(tenantId, fromDate, toDate);
        return aggregatesService.compute(query);
    }
}
