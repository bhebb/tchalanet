package com.tchalanet.server.features.pagemodel_backup.shared.block;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultsByGameBlock(List<GameResults> games) {
  public record GameResults(
      String gameCode,
      String gameNameKey,

      // last result
      Instant lastDrawDateTime,
      List<String> lastMainNumbers,
      List<String> lastExtraNumbers,
      Long lastJackpotAmount,
      String currencyCode,

      // link to history
      String historyUrl,

      // next draw / countdown
      NextDrawInfo nextDraw) {}

  public record NextDrawInfo(
      Instant scheduledAt,
      String drawLabel, // e.g. "Midi", "Soir", "US Midday", optional
      boolean isOpen, // betting window open?
      boolean isClosingSoon // optional flag for UI
      ) {}
}
