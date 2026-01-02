package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.handler.GetNextDrawsHandler;
import com.tchalanet.server.core.draw.application.query.handler.ListActiveDrawChannelsHandler;
import com.tchalanet.server.core.draw.application.query.handler.ListLastDaysDrawResultsHandler;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListActiveDrawChannelsQuery;
import com.tchalanet.server.core.draw.application.query.model.ListLastDaysDrawResultsQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedResultsByGameAggregator {

  private final ListActiveDrawChannelsHandler listActiveDrawChannelsHandler;
  private final ListLastDaysDrawResultsHandler listLastDaysDrawResultsHandler;
  private final GetNextDrawsHandler getNextDrawsHandler;
  private final Clock clock;

  public ResultsByGameBlock buildResultsBlock(TenantId tenantId) {
    // 1) Channels actifs
    var channels = listActiveDrawChannelsHandler.handle(new ListActiveDrawChannelsQuery(tenantId));

    if (channels.isEmpty()) {
      return new ResultsByGameBlock(List.of());
    }

    var channelCodes =
        channels.stream()
            .map(DrawChannelSummary::channelCode) // adapte au vrai nom
            .toList();

    // 2) Next draws pour tous ces channels
    var nextDraws =
        getNextDrawsHandler.handle(new GetNextDrawsQuery(tenantId, ZonedDateTime.now(clock), 10));

    var nextByChannel =
        nextDraws.stream().collect(Collectors.toMap(d -> d.drawChannel().id(), d -> d));

    // 3) Last result par channel (sur X jours)
    int lastDays = 7; // param configurable plus tard
    List<ResultsByGameBlock.GameResults> games = new ArrayList<>();

    for (var channel : channels) {
      var results =
          listLastDaysDrawResultsHandler.handle(
              new ListLastDaysDrawResultsQuery(
                  tenantId, channel.channelCode(), lastDays, null, null));

      if (results == null || results.isEmpty()) {
        continue;
      }

      var last =
          results.stream()
              .max(Comparator.comparing(DrawResult::occurredAt)) // adapte getter
              .orElseThrow();

      var next = nextByChannel.get(channel.channelCode());

      games.add(toGameResults(channel, last, next));
    }

    return new ResultsByGameBlock(games);
  }

  private ResultsByGameBlock.GameResults toGameResults(
      DrawChannelSummary channel, DrawResult last, Draw next) {
    var nextInfo =
        next != null
            ? new ResultsByGameBlock.NextDrawInfo(
                next.scheduledAt().toInstant(), // adapte
                null, // ex: "Midi"/"Soir"
                next.status() == DrawStatus.OPEN, // ou statut == OPEN
                false // isClosingSoon: TODO plus tard
                )
            : null;

    return new ResultsByGameBlock.GameResults(
        channel.channelCode(),
        channel.channelName(), // clé i18n
        last.occurredAt(), // adapte
        last.numbersMain(),
        last.numbersExtra(),
        null, // si dispo
        null, // ou via tenant
        nextInfo);
  }
}
