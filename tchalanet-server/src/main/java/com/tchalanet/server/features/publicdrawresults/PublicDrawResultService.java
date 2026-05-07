package com.tchalanet.server.features.publicdrawresults;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.drawresult.application.query.model.ListPublicDrawResultSlotDetailsQuery;
import com.tchalanet.server.core.drawresult.application.query.model.ListPublicDrawResultSlotsQuery;
import com.tchalanet.server.core.drawresult.application.query.model.SearchPublicDrawResultsQuery;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSearchCriteria;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSlotsResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicDrawResultService {

  private final QueryBus queryBus;
  private final PublicDrawResultViewMapper mapper;

  public PublicDrawResultSlotsResponse slots(List<String> slotKeys, String provider) {
    var views =
        queryBus.send(
            new ListPublicDrawResultSlotsQuery(normalizeSlotKeys(slotKeys), provider));

    return mapper.toSlotsResponse(views);
  }

  public PublicDrawResultSlotsResponse details(
      List<String> slotKeys, String provider, int historyLimit) {
    var views =
        queryBus.send(
            new ListPublicDrawResultSlotDetailsQuery(
                normalizeSlotKeys(slotKeys), provider, historyLimit));

    return mapper.toDetailsResponse(views);
  }

  public PublicDrawResultListResponse history(PublicDrawResultSearchCriteria criteria) {
    var page =
        queryBus.send(
            new SearchPublicDrawResultsQuery(
                normalizeSlotKeys(criteria.slotKeys()),
                criteria.provider(),
                criteria.from(),
                criteria.to(),
                criteria.pageable()));

    return mapper.toHistoryResponse(page);
  }

  private static List<String> normalizeSlotKeys(List<String> slotKeys) {
    if (slotKeys == null || slotKeys.isEmpty()) {
      return List.of();
    }

    return slotKeys.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(value -> value.trim().toUpperCase())
        .distinct()
        .toList();
  }
}
