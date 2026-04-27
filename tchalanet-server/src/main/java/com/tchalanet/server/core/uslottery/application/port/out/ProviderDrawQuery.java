package com.tchalanet.server.core.uslottery.application.port.out;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record ProviderDrawQuery(
    LocalDate drawDate,
    Set<String> channelCodes, // empty => "tout ce que le provider sait"
    int maxDraws,
    boolean force,
    boolean dryRun) {
  public ProviderDrawQuery {
    Objects.requireNonNull(drawDate, "drawDate required");
    if (maxDraws <= 0) throw new IllegalArgumentException("maxDraws must be > 0");
    if (channelCodes == null) channelCodes = Set.of();

    channelCodes =
        channelCodes.stream()
            .filter(Objects::nonNull)
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .filter(s -> !s.isBlank())
            .collect(Collectors.toUnmodifiableSet());
  }
}
