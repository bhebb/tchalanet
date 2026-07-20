package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.SalesAnalyticsSnapshotReaderPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** Exposes immutable sales snapshots without leaking sales persistence into core.analytics. */
@UseCase
@RequiredArgsConstructor
public class GetSalesAnalyticsTicketSnapshotsQueryHandler
    implements QueryHandler<
        GetSalesAnalyticsTicketSnapshotsQuery, List<SalesAnalyticsTicketSnapshot>> {

  private final SalesAnalyticsSnapshotReaderPort reader;

  @Override
  public List<SalesAnalyticsTicketSnapshot> handle(GetSalesAnalyticsTicketSnapshotsQuery query) {
    return reader.findTicketSnapshots(query);
  }
}
