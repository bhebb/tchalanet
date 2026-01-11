package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock;
import com.tchalanet.server.features.publicdraw.application.query.model.GetLatestPublicDrawResultsQuery;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicLatestDrawResultsResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedResultsByGameAggregator {

  private final QueryBus queryBus;
  private final Clock clock; // inject Clock for testability

  public ResultsByGameBlock buildResultsBlock(String currentLang) {
    // locale kept for future i18n needs (we return keys)
    Locale locale =
        (currentLang == null || currentLang.isBlank())
            ? Locale.getDefault()
            : Locale.forLanguageTag(currentLang);

    int limitPerSlot = 1;
    List<PublicLatestDrawResultsResponse> latest =
        queryBus.send(new GetLatestPublicDrawResultsQuery(limitPerSlot));

    List<ResultsByGameBlock.GameResults> games =
        (latest == null) ? List.of() : toGames(latest, locale);

    return new ResultsByGameBlock(games);
  }

  private List<ResultsByGameBlock.GameResults> toGames(
      List<PublicLatestDrawResultsResponse> latestResponses, Locale locale) {

    return latestResponses.stream()
        .filter(r -> r != null && r.results() != null && !r.results().isEmpty())
        .map(this::mapLatestToGameResults)
        .collect(Collectors.toList());
  }

  private ResultsByGameBlock.GameResults mapLatestToGameResults(
      PublicLatestDrawResultsResponse latest) {

    var item = latest.results().get(0);

    // Use slotKey as "gameCode" for this public UI block
    var slotKey = nz(latest.slotKey());
    var gameCode = slotKey;
    var gameNameKey = slotNameKey(slotKey);

    // Last result numbers = Haiti lots (lot1..lot4)
    List<String> lastMain =
        List.of(nn(item.lot1()), nn(item.lot2()), nn(item.lot3()), nn(item.lot4())).stream()
            .filter(s -> !s.isBlank())
            .toList();

    List<String> lastExtra = List.of(); // MVP: no extras in this block

    String historyUrl = "/public/results/" + slotKey;

    // Next draw info derived from nextScheduledAt
    ResultsByGameBlock.NextDrawInfo next = buildNextDrawInfo(latest);

    return new ResultsByGameBlock.GameResults(
        gameCode,
        gameNameKey,
        item.occurredAt(), // Instant UTC
        lastMain,
        lastExtra,
        null, // jackpot
        null, // currency
        historyUrl,
        next);
  }

  private ResultsByGameBlock.NextDrawInfo buildNextDrawInfo(
      PublicLatestDrawResultsResponse latest) {
    Instant nextAt = latest.nextScheduledAt();
    if (nextAt == null) return null;

    Instant now = Instant.now(clock);

    boolean isOpen = now.isBefore(nextAt);

    boolean isClosingSoon = false;
    try {
      long seconds = Duration.between(now, nextAt).getSeconds();
      // MVP rule: "closing soon" if <= 5 minutes
      isClosingSoon = seconds >= 0 && seconds <= 300;
    } catch (Exception ignore) {
      // keep false
    }

    // drawLabel: keep it optional. We can show drawTime or provider/slot hint in UI later.
    String drawLabel = buildNextDrawLabel(latest);

    return new ResultsByGameBlock.NextDrawInfo(nextAt, drawLabel, isOpen, isClosingSoon);
  }

  private String buildNextDrawLabel(PublicLatestDrawResultsResponse latest) {
    // Keep it minimal for MVP:
    // ex: "14:30" if present, else null.
    String t = nn(latest.drawTime());
    if (!t.isBlank()) return t;

    // fallback: provider
    String p = nn(latest.provider());
    return p.isBlank() ? null : p;
  }

  // i18n key for frontend
  private static String slotNameKey(String slotKey) {
    if (slotKey == null || slotKey.isBlank()) return "public_draw.slot.unknown";
    return "public_draw.slot." + slotKey.trim().toLowerCase(Locale.ROOT);
  }

  private static String nz(String s) {
    return (s == null) ? "" : s.trim();
  }

  private static String nn(String s) {
    return (s == null) ? "" : s.trim();
  }
}
