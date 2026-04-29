package com.tchalanet.server.features.publicdraw.app;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.features.publicdraw.PublicDrawResultMapper;
import com.tchalanet.server.features.publicdraw.model.PublicLatestDrawResultsResponse;
import com.tchalanet.server.features.publicdraw.persistence.PublicDrawResultRow;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicLatestDrawResultsService {

  private final PublicDrawResultReader reader;
  private final PublicDrawResultMapper mapper;
  private final ResultSlotCatalog slotReader;
  private final NextDrawCalculator nextCalc;

  public List<PublicLatestDrawResultsResponse> latest(int limitPerSlot) {
    int limit = Math.max(1, Math.min(10, limitPerSlot));

    List<PublicDrawResultRow> rows = reader.latest(limit);

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
