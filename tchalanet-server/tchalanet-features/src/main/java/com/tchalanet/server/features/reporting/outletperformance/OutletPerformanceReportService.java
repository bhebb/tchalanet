package com.tchalanet.server.features.reporting.outletperformance;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.query.GetOutletReportQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Outlet performance report service — delegates to core.analytics via QueryBus. */
@Service
@RequiredArgsConstructor
public class OutletPerformanceReportService {

  private final QueryBus queryBus;

  public OutletPerformanceReportResponse getReport(OutletPerformanceReportCriteria criteria) {
    TenantId tenantId = criteria.tenantId() != null ? TenantId.of(criteria.tenantId()) : null;

    var analyticsLines = queryBus.ask(new GetOutletReportQuery(
        tenantId, criteria.fromDate(), criteria.toDate(), criteria.gameCode()));

    // Map from core.analytics OutletReportLine to local OutletPerformanceLine
    var lines = analyticsLines.stream()
        .map(l -> new OutletPerformanceLine(
            l.outletId().value(),
            l.outletCode(),
            l.outletName(),
            l.gameCode(),
            l.ticketsSold(),
            l.totalSales(),
            l.totalPayout(),
            l.netRevenue()))
        .toList();

    return new OutletPerformanceReportResponse(
        criteria.fromDate(), criteria.toDate(), criteria.gameCode(), lines);
  }
}
