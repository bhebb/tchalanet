package com.tchalanet.server.features.reporting.salesreport;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.query.GetSalesReportQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Sales report service — delegates to core.analytics via QueryBus. */
@Service
@RequiredArgsConstructor
public class SalesReportService {

  private final QueryBus queryBus;

  public SalesReportResponse getReport(SalesReportCriteria criteria) {
    TenantId tenantId = criteria.tenantId() != null ? TenantId.of(criteria.tenantId()) : null;

    List<com.tchalanet.server.core.analytics.api.model.SalesReportLine> analyticsLines =
        queryBus.ask(new GetSalesReportQuery(
            tenantId, criteria.fromDate(), criteria.toDate(), criteria.gameCode()));

    // Map from core.analytics model to local features.reporting model
    List<SalesReportLine> lines = analyticsLines.stream()
        .map(l -> new SalesReportLine(
            l.date(), l.gameCode(), l.ticketsSold(),
            l.totalSales(), l.totalPayout(), l.netRevenue()))
        .toList();

    return new SalesReportResponse(
        criteria.fromDate(), criteria.toDate(), criteria.gameCode(), lines);
  }
}
