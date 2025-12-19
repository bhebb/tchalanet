package com.tchalanet.server.core.uslottery.infra.config;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Registry construit depuis application.yaml (tch.us-lottery.providers.*.games). */
@Component
@RequiredArgsConstructor
public class UsLotteryGameRegistry {

  private final UsLotteryProperties properties;

  public Optional<GameInfo> resolve(String channelCode) {
    String lookup = normalize(channelCode);
    if (lookup.isBlank()) return Optional.empty();

    Map<String, UsLotteryProperties.ProviderProperties> providers = properties.getProviders();
    if (providers == null || providers.isEmpty()) return Optional.empty();

    for (Map.Entry<String, UsLotteryProperties.ProviderProperties> entry : providers.entrySet()) {
      Optional<GameInfo> match = tryResolveInProvider(entry.getKey(), entry.getValue(), lookup);
      if (match.isPresent()) return match;
    }

    return Optional.empty();
  }

  private Optional<GameInfo> tryResolveInProvider(
      String providerKey, UsLotteryProperties.ProviderProperties providerProps, String lookup) {
    if (providerProps == null || !providerProps.isEnabled() || providerProps.getGames() == null)
      return Optional.empty();

    for (UsLotteryProperties.GameProps g : providerProps.getGames()) {
      if (g == null) continue;
      if (lookup.equalsIgnoreCase(normalize(g.getCode()))) {
        ZoneId zone = parseZone(providerProps.getTimezone());
        LocalTime drawTime = parseLocalTime(g.getDrawTime());
        return Optional.of(new GameInfo(providerKey, safe(g.getExternalKey()), drawTime, zone));
      }
    }

    return Optional.empty();
  }

  private String normalize(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  private ZoneId parseZone(String timezone) {
    String tz = safe(timezone);
    return tz.isBlank() ? ZoneId.of("UTC") : ZoneId.of(tz);
  }

  private LocalTime parseLocalTime(String time) {
    String t = safe(time);
    return t.isBlank() ? null : LocalTime.parse(t);
  }

  private String safe(String s) {
    return s == null ? "" : s.trim();
  }

  public record GameInfo(String provider, String externalKey, LocalTime drawTime, ZoneId timezone) {
    public GameInfo {
      Objects.requireNonNull(provider, "provider required");
      Objects.requireNonNull(externalKey, "externalKey required");
      Objects.requireNonNull(timezone, "timezone required");
    }
  }
}
