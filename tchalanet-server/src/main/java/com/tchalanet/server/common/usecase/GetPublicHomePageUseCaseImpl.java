package com.tchalanet.server.common.usecase;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.common.web.dto.PublicHomeData;
import com.tchalanet.server.draw.domain.dto.PublicDrawSummary;
import com.tchalanet.server.game.domain.usecase.ListPublicGamesUseCase;
import com.tchalanet.server.tenant.domain.usecase.ListPublicPlansUseCase;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Implémentation du use case d'orchestration de la page publique. Agrège les plans publics, les
 * jeux visibles, les tirages du jour et le prochain tirage pour un tenant éventuel.
 */
@UseCase
@RequiredArgsConstructor
public class GetPublicHomePageUseCaseImpl implements GetPublicHomePageUseCase {

  private final Optional<ListPublicPlansUseCase> listPublicPlansUseCase;
  private final Optional<ListPublicGamesUseCase> listPublicGamesUseCase;
  private final Optional<com.tchalanet.server.common.usecase.ListPublicNewsUseCase>
      listPublicNewsUseCase;
  private final Optional<com.tchalanet.server.draw.domain.usecase.GetPublicDrawSummaryUseCase>
      drawSummaryUseCase;

  @Override
  public PublicHomeData get(UUID tenantId) {
    // plans (domain objects) -> convert to map for front
    List<com.tchalanet.server.tenant.domain.model.Plan> planList =
        listPublicPlansUseCase.map(p -> p.listForTenant(tenantId)).orElseGet(List::of);

    List<Map<String, Object>> plans =
        planList.stream()
            .map(
                pl ->
                    Map.<String, Object>of(
                        "id", pl.id(),
                        "code", pl.code(),
                        "name", pl.name(),
                        "priceAmount", pl.priceAmount(),
                        "currency", pl.currency(),
                        "features", pl.features()))
            .collect(Collectors.toList());

    // games: ListPublicGamesUseCase returns List<Map<String,Object>>
    List<Map<String, Object>> games =
        listPublicGamesUseCase.map(g -> g.listPublicGames(tenantId)).orElseGet(List::of);

    // draws today and next draw via draw summary use case (preferred)
    List<Map<String, Object>> draws = List.of();
    Map<String, Object> nextDraw = Map.of();
    if (drawSummaryUseCase.isPresent()) {
      PublicDrawSummary summary = drawSummaryUseCase.get().getSummaryForTenant(tenantId);
      // adapt DTOs to simple map shapes used by front
      draws =
          summary.todayResults().stream()
              .map(
                  d ->
                      Map.<String, Object>of(
                          "id", d.id(),
                          "gameCode", d.channelCode(),
                          "scheduledAt", d.scheduledAt(),
                          "status", d.resultAt() != null ? "RESULTED" : "SCHEDULED",
                          "result",
                              d.numbers() == null ? Map.of() : Map.of("numbers", d.numbers())))
              .collect(Collectors.toList());

      // next draw: pick first next draw or transform to expected map
      var nextList = summary.nextByChannel();
      if (!nextList.isEmpty()) {
        var nd = nextList.get(0);
        nextDraw =
            Map.of(
                "channelId", nd.channelId(),
                "channelCode", nd.channelCode(),
                "scheduledAt", nd.scheduledAt(),
                "cutoffAt", nd.cutoffAt());
      }
    } else {
      // fallback: use the old listTodayDraws use case
      var today = LocalDate.now();
      draws = List.of(new HashMap<>());
    }

    // news
    List<Map<String, Object>> news =
        listPublicNewsUseCase.map(ListPublicNewsUseCase::listPublicNews).orElseGet(List::of);

    return new PublicHomeData(tenantId, plans, games, draws, nextDraw, news);
  }
}
