package com.tchalanet.server.core.analytics.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.model.OutletReportLine;
import com.tchalanet.server.core.analytics.api.query.GetOutletReportQuery;
import com.tchalanet.server.core.analytics.internal.infra.persistence.OutletReportAnalyticsReader;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Handles {@link GetOutletReportQuery} — reads outlet performance metrics.
 */
@UseCase
@RequiredArgsConstructor
public class GetOutletReportQueryHandler
    implements QueryHandler<GetOutletReportQuery, List<OutletReportLine>> {

  private final OutletReportAnalyticsReader reader;

  @Override
  public List<OutletReportLine> handle(GetOutletReportQuery query) {
    if (query.tenantId() == null) {
      return List.of();
    }
    return reader.findOutletPerformance(
        query.tenantId().value(), query.fromDate(), query.toDate(), query.gameCode());
  }
}
