package com.tchalanet.server.features.publicdraw.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.core.resultslot.api.ResultSlotView;
import com.tchalanet.server.features.publicdraw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.features.publicdraw.application.query.model.GetLatestPublicDrawResultsQuery;
import com.tchalanet.server.features.publicdraw.application.service.NextDrawCalculator;
import com.tchalanet.server.features.publicdraw.infra.persistence.PublicDrawResultRow;
import com.tchalanet.server.features.publicdraw.infra.web.mapper.PublicDrawResultMapper;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicLatestDrawResultsResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetLatestPublicDrawResultsQueryHandler
    implements QueryHandler<
        GetLatestPublicDrawResultsQuery, List<PublicLatestDrawResultsResponse>> {

  private final PublicDrawResultPort port;
  private final PublicDrawResultMapper mapper;
  private final ResultSlotCatalog slotReader;
  private final NextDrawCalculator nextCalc;

  @Override
  public List<PublicLatestDrawResultsResponse> handle(GetLatestPublicDrawResultsQuery q) {
    int limit = Math.max(1, Math.min(10, q.limitPerChannel()));

    List<PublicDrawResultRow> rows = port.latest(limit);

    Map<String, List<PublicDrawResultRow>> grouped =
        rows.stream().collect(Collectors.groupingBy(PublicDrawResultRow::getSlotKey));

    var slotsByKey =
        slotReader.listActive().stream().collect(Collectors.toMap(ResultSlotView::slotKey, s -> s));

    return grouped.entrySet().stream()
        .map(
            e -> {
              String slotKey = e.getKey();
              var slot = slotsByKey.get(slotKey);

              String provider = slot != null ? slot.provider() : e.getValue().get(0).getProvider();
              String timezone =
                  slot != null ? slot.timezone().getId() : e.getValue().get(0).getSlotTimezone();
              String drawTime =
                  slot != null && slot.drawTime() != null
                      ? slot.drawTime().toString()
                      : (e.getValue().get(0).getSlotDrawTime() == null
                          ? null
                          : e.getValue().get(0).getSlotDrawTime().toString());

              var next =
                  nextCalc.nextScheduledAt(
                      timezone,
                      slot != null ? slot.drawTime() : e.getValue().get(0).getSlotDrawTime(),
                      null);

              var items = e.getValue().stream().map(mapper::toItem).toList();

              return new PublicLatestDrawResultsResponse(
                  slotKey, provider, timezone, drawTime, next, items);
            })
        .toList();
  }
}
