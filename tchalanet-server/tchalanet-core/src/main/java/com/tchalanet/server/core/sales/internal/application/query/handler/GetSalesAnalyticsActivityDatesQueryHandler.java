package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.query.GetSalesAnalyticsActivityDatesQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.SalesAnalyticsActivityReaderPort;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/** Exposes source activity dates without leaking sales persistence into core.analytics. */
@UseCase
@RequiredArgsConstructor
public class GetSalesAnalyticsActivityDatesQueryHandler
    implements QueryHandler<GetSalesAnalyticsActivityDatesQuery, Set<LocalDate>> {

  private final SalesAnalyticsActivityReaderPort reader;

  @Override
  public Set<LocalDate> handle(GetSalesAnalyticsActivityDatesQuery query) {
    return reader.findActivityDates(query);
  }
}
