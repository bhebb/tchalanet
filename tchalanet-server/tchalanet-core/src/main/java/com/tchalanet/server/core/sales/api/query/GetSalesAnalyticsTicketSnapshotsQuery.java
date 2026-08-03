package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import java.time.Instant;
import java.util.List;

/** Reads ticket snapshots with analytics lifecycle activity in a tenant-local date window. */
public record GetSalesAnalyticsTicketSnapshotsQuery(TenantId tenantId, Instant from, Instant to)
    implements Query<List<SalesAnalyticsTicketSnapshot>> {

  public GetSalesAnalyticsTicketSnapshotsQuery {
    if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
    if (from == null || to == null) throw new IllegalArgumentException("from/to must not be null");
    if (!from.isBefore(to)) throw new IllegalArgumentException("from must be before to");
  }
}
