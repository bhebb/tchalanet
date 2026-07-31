package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsActivityDatesQuery;
import java.time.LocalDate;
import java.util.Set;

/** Sales-owned read boundary for analytics source activity coverage. */
public interface SalesAnalyticsActivityReaderPort {

  Set<LocalDate> findActivityDates(GetSalesAnalyticsActivityDatesQuery query);
}
