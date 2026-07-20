package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotsQuery;
import java.util.List;

/** Sales-owned read boundary for analytics reconciliation source snapshots. */
public interface SalesAnalyticsSnapshotReaderPort {

  List<SalesAnalyticsTicketSnapshot> findTicketSnapshots(
      GetSalesAnalyticsTicketSnapshotsQuery query);
}
