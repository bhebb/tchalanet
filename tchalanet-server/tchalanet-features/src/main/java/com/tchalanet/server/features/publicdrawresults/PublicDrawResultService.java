package com.tchalanet.server.features.publicdrawresults;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.api.query.GetPublicDrawResultDetailByIdQuery;
import com.tchalanet.server.core.drawresult.api.query.ListPublicDrawResultSlotsQuery;
import com.tchalanet.server.core.drawresult.api.query.SearchPublicDrawResultsQuery;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultDetailResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultHistoryResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultLatestResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSearchCriteria;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicDrawResultService {

  private final QueryBus queryBus;
  private final PublicDrawResultViewMapper mapper;
  private final Clock clock;

  /**
   * Dernier résultat + prochain tirage par slot public actif.
   * Alimente le widget home.
   *
   * @param slotKeys Liste de slots filtrés (null ou vide = tous les slots actifs)
   * @param provider Provider filtré (null = tous les providers)
   * @param limit Nombre maximum de résultats (null = tous les slots, sinon limité)
   */
  public PublicDrawResultLatestResponse latest(
      List<String> slotKeys, String provider, Integer limit) {
    var views =
        queryBus.ask(
            new ListPublicDrawResultSlotsQuery(normalizeSlotKeys(slotKeys), provider));
    return mapper.toLatestResponse(views, limit, clock.instant());
  }

  /**
   * Historique paginé — filtre par dates, slot et provider.
   * Alimente la page publique {@code /public/results}.
   */
  public PublicDrawResultHistoryResponse history(PublicDrawResultSearchCriteria criteria) {
    var page =
        queryBus.ask(
            new SearchPublicDrawResultsQuery(
                normalizeSlotKeys(criteria.slotKeys()),
                criteria.provider(),
                criteria.from(),
                criteria.to(),
                criteria.pageable()));
    return mapper.toHistoryResponse(page);
  }

  /**
   * Détail public d'un résultat identifié par son {@code DrawResultId} opaque.
   */
  public PublicDrawResultDetailResponse detail(DrawResultId drawResultId) {
    var view = queryBus.ask(new GetPublicDrawResultDetailByIdQuery(drawResultId));
    return mapper.toDetailResponse(view);
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
