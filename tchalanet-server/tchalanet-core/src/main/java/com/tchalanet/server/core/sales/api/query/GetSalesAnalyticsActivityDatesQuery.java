package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

/** Returns business dates with source ticket activity relevant to analytics coverage. */
public record GetSalesAnalyticsActivityDatesQuery(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    Instant from,
    Instant to,
    ZoneId businessZone)
    implements Query<Set<LocalDate>> {

  public GetSalesAnalyticsActivityDatesQuery {
    if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
    if (from == null || to == null) throw new IllegalArgumentException("from/to must not be null");
    if (!from.isBefore(to)) throw new IllegalArgumentException("from must be before to");
    if (businessZone == null) throw new IllegalArgumentException("businessZone must not be null");
  }
}
