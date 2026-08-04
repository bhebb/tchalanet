package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotQuery;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsTicketSnapshotsQuery;
import java.util.List;
import java.util.Optional;

/** Sales-owned read boundary for analytics reconciliation source snapshots. */
public interface SalesAnalyticsSnapshotReaderPort {

  List<SalesAnalyticsTicketSnapshot> findTicketSnapshots(
      GetSalesAnalyticsTicketSnapshotsQuery query);

  Optional<SalesAnalyticsTicketSnapshot> findTicketSnapshot(
      GetSalesAnalyticsTicketSnapshotQuery query);
}
