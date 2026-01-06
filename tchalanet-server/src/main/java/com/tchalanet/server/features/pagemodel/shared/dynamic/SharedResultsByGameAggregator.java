package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.draw.application.print.DrawChannelLabelResolver;
import com.tchalanet.server.core.draw.application.query.model.GetLatestPublicDrawResultsQuery;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultItemResponse;
import com.tchalanet.server.core.draw.infra.web.model.PublicLatestDrawResultsResponse;
import com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedResultsByGameAggregator {

  private final QueryBus queryBus;
  private final DrawChannelLabelResolver channelLabelResolver;
  private static final Logger log = LoggerFactory.getLogger(SharedResultsByGameAggregator.class);

  public ResultsByGameBlock buildResultsBlock(String currentLang) {
    Locale locale =
        currentLang == null || currentLang.isBlank()
            ? Locale.getDefault()
            : Locale.forLanguageTag(currentLang);

    // 1) Derniers résultats : utiliser le handler public pour obtenir les derniers résultats par
    // canal
    int limitPerChannel = 1; // on ne veut que le dernier résultat
    List<PublicLatestDrawResultsResponse> latestResponses =
        queryBus.send(new GetLatestPublicDrawResultsQuery(limitPerChannel));

    if (latestResponses != null) {
      latestResponses.forEach(
          latest -> {
            if (latest.nextScheduledAt() == null) {
              log.warn(
                  "No nextScheduledAt for channel {} (drawTime={})",
                  latest.channelCode(),
                  latest.drawTime());
            } else {
              log.debug(
                  "NextScheduledAt for channel {} = {}",
                  latest.channelCode(),
                  latest.nextScheduledAt());
            }
          });
    }

    List<ResultsByGameBlock.GameResults> games =
        (latestResponses == null) ? List.of() : toGameResults(latestResponses, locale);

    return new ResultsByGameBlock(games);
  }

  private List<ResultsByGameBlock.GameResults> toGameResults(
      List<PublicLatestDrawResultsResponse> latestResponses, Locale locale) {
    return latestResponses.stream()
        .filter(latest -> latest != null && latest.results() != null && !latest.results().isEmpty())
        .map(
            latest ->
                toGameResultsFromLatest(
                    latest, latest.results().stream().findFirst().get(), locale))
        .collect(Collectors.toList());
  }

  private ResultsByGameBlock.GameResults toGameResultsFromLatest(
      PublicLatestDrawResultsResponse latest, PublicDrawResultItemResponse item, Locale locale) {
    // parse draw time if available (expected format HH:mm)
    LocalTime drawTimeLocal = null;
    if (latest.drawTime() != null && !latest.drawTime().isBlank()) {
      try {
        drawTimeLocal = LocalTime.parse(latest.drawTime());
      } catch (Exception ignore) {
        // ignore parse errors and keep null
      }
    }

    String channelLabel = channelLabelResolver.resolve(latest.channelName(), drawTimeLocal, locale);

    String historyUrl = "/public/results/" + latest.channelCode();

    List<String> main =
        item.numbersMain() == null
            ? List.of()
            : item.numbersMain().stream().map(Object::toString).collect(Collectors.toList());
    List<String> extra =
        item.numbersExtra() == null
            ? List.of()
            : item.numbersExtra().stream().map(Object::toString).collect(Collectors.toList());

    // build nextInfo from latest response if provided
    var nextInfo =
        (latest.nextScheduledAt() == null)
            ? null
            : new com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock
                .NextDrawInfo(
                latest.nextScheduledAt(),
                latest.nextDrawLabel(),
                Boolean.TRUE.equals(latest.nextIsOpen()),
                Boolean.TRUE.equals(latest.nextIsClosingSoon()));

    return new ResultsByGameBlock.GameResults(
        latest.channelCode(),
        channelLabel,
        item.occurredAt(),
        main,
        extra,
        null,
        null,
        historyUrl,
        nextInfo);
  }
}
