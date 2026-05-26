package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletKind;

/** Filters for {@link ListOutletsQuery}. All fields nullable = "no filter". */
public record OutletSearchCriteria(
    String q,
    Boolean active,
    Boolean salesBlocked,
    Boolean dayClosed,
    Boolean outletBlocked,
    OutletKind kind,
    SalesZoneId zoneId) {

  public static OutletSearchCriteria empty() {
    return new OutletSearchCriteria(null, null, null, null, null, null, null);
  }
}
