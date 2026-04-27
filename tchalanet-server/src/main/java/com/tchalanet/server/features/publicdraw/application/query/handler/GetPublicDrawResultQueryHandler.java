package com.tchalanet.server.features.publicdraw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.features.publicdraw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.features.publicdraw.application.query.model.GetPublicDrawResultQuery;
import com.tchalanet.server.features.publicdraw.application.service.NextDrawCalculator;
import com.tchalanet.server.features.publicdraw.infra.persistence.PublicDrawResultRow;
import com.tchalanet.server.features.publicdraw.infra.web.mapper.PublicDrawResultMapper;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicDrawResultDetailsResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPublicDrawResultQueryHandler
    implements QueryHandler<GetPublicDrawResultQuery, PublicDrawResultDetailsResponse> {

  private final PublicDrawResultPort port;
  private final PublicDrawResultMapper mapper;
  private final TimeProvider time;
  private final NextDrawCalculator nextDrawCalculator;

  @Override
  public PublicDrawResultDetailsResponse handle(GetPublicDrawResultQuery q) {
    PublicDrawResultRow row =
        port.findOne(q.slotKey(), q.drawDate())
            .orElseThrow(() -> new IllegalArgumentException("public draw result not found"));

    var item = mapper.toItem(row);

    Instant next =
        nextDrawCalculator.nextScheduledAt(
            row.getSlotTimezone(), row.getSlotDrawTime(), row.getDaysOfWeek());

    LocalDate nextDate = null;
    String nextTime = null;
    try {
      var zone = ZoneId.of(row.getSlotTimezone());
      nextDate = next == null ? null : next.atZone(zone).toLocalDate();
      nextTime = row.getSlotDrawTime() == null ? null : row.getSlotDrawTime().toString();
    } catch (Exception ignore) {
    }

    return new PublicDrawResultDetailsResponse(item, next, nextDate, nextTime);
  }
}
