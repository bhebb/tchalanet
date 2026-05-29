package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.SalesReportLine;
import com.tchalanet.server.core.analytics.api.query.GetSalesReportQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.SalesReportAnalyticsReader;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Handles {@link GetSalesReportQuery} — reads per-day per-game sales breakdown.
 */
@UseCase
@RequiredArgsConstructor
public class GetSalesReportQueryHandler
    implements QueryHandler<GetSalesReportQuery, List<SalesReportLine>> {

  private final SalesReportAnalyticsReader reader;

  @Override
  public List<SalesReportLine> handle(GetSalesReportQuery query) {
    if (query.tenantId() == null) {
      return List.of();
    }
    return reader.findSalesByPeriodAndGame(
        query.tenantId().value(), query.fromDate(), query.toDate(), query.gameCode());
  }
}
