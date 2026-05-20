package com.tchalanet.server.core.outlet.api.query;

/** Filters for {@link ListOutletsQuery}. All fields nullable = "no filter". */
public record OutletSearchCriteria(
    String q, Boolean active, Boolean salesBlocked, Boolean dayClosed) {

  public static OutletSearchCriteria empty() {
    return new OutletSearchCriteria(null, null, null, null);
  }
}
