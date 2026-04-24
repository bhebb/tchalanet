package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.SalesTicketAdminPort;
import com.tchalanet.server.core.outlet.application.port.out.SessionLookupPort;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletDailySummaryQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletDailySummary;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetOutletDailySummaryQueryHandler
    implements QueryHandler<GetOutletDailySummaryQuery, OutletDailySummary> {

  private final SalesTicketAdminPort salesAdmin;
  private final SessionLookupPort sessionLookup;
  private final OutletReaderPort outletReader;

  @Override
  public OutletDailySummary handle(GetOutletDailySummaryQuery query) {
    var outlet = outletReader.getRequired(query.outletId());

    var zone = ZoneId.of(outlet.timezone());
    var from = query.date().atStartOfDay(zone).toInstant();
    var to = query.date().plusDays(1).atStartOfDay(zone).toInstant();

    var stats = salesAdmin.getCloseStats(query.outletId(), from, to);
    var sessions = sessionLookup.findSessionIds(query.outletId(), from, to);

    return new OutletDailySummary(
        query.date(),
        stats.total(),
        stats.sold(),
        stats.voided(),
        stats.resultedWin(),
        stats.resultedLoss(),
        stats.paid(),
        sessions.size(),
        outlet.name(),
        outlet.salesBlocked());
  }
}
