package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import java.util.Optional;

/** Reads one immutable ticket snapshot for lifecycle-driven analytics projection updates. */
public record GetSalesAnalyticsTicketSnapshotQuery(TicketId ticketId)
    implements Query<Optional<SalesAnalyticsTicketSnapshot>> {

  public GetSalesAnalyticsTicketSnapshotQuery {
    if (ticketId == null) throw new IllegalArgumentException("ticketId must not be null");
  }
}
