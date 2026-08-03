package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.analytics.SalesAnalyticsTicketSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Reads ticket snapshots with analytics lifecycle activity in a tenant-local date window. */
public record GetSalesAnalyticsTicketSnapshotsQuery(
    TenantId tenantId, Instant from, Instant to, LocalDate fromDate, LocalDate toDate)
    implements Query<List<SalesAnalyticsTicketSnapshot>> {

  public GetSalesAnalyticsTicketSnapshotsQuery {
    if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
    if (from == null || to == null) throw new IllegalArgumentException("from/to must not be null");
    if (fromDate == null || toDate == null) {
      throw new IllegalArgumentException("fromDate/toDate must not be null");
    }
    if (!from.isBefore(to)) throw new IllegalArgumentException("from must be before to");
    if (fromDate.isAfter(toDate)) throw new IllegalArgumentException("fromDate must not be after toDate");
  }
}
