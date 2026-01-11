package com.tchalanet.server.core.uslottery.infra.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsLotteryGameRegistry {

  private final UsLotteryProperties props;

  public Optional<GameInfo> resolve(String gameCode) {
    String code = norm(gameCode);
    if (code.isBlank()) return Optional.empty();
    if (props == null || !props.isEnabled()) return Optional.empty();

    var providers = props.getProviders();
    if (providers == null || providers.isEmpty()) return Optional.empty();

    for (var e : providers.entrySet()) {
      String providerKey = norm(e.getKey()).toLowerCase(Locale.ROOT);
      var p = e.getValue();
      if (p == null || !p.isEnabled()) continue;

      ZoneId tz = parseZone(p.getTimezone());
      List<UsLotteryProperties.GameProps> games = p.getGames();
      if (games == null) continue;

      for (var g : games) {
        if (g == null || !g.isActive()) continue;
        if (code.equals(norm(g.getCode()))) {
          return Optional.of(
              new GameInfo(
                  providerKey,
                  safe(g.getExternalKey()),
                  parseTime(g.getDrawTime()),
                  tz,
                  true,
                  parseDays(g.getDays())));
        }
      }
    }
    return Optional.empty();
  }

  public List<GameInfo> listByProvider(String providerKey) {
    String pk = norm(providerKey).toLowerCase(Locale.ROOT);
    if (pk.isBlank()) return List.of();
    if (props == null || !props.isEnabled()) return List.of();

    var providers = props.getProviders();
    var p = providers == null ? null : providers.get(pk);
    if (p == null || !p.isEnabled() || p.getGames() == null) return List.of();

    ZoneId tz = parseZone(p.getTimezone());
    List<GameInfo> out = new ArrayList<>();
    for (var g : p.getGames()) {
      if (g == null || !g.isActive()) continue;
      out.add(
          new GameInfo(
              pk,
              safe(g.getExternalKey()),
              parseTime(g.getDrawTime()),
              tz,
              true,
              parseDays(g.getDays())));
    }
    return List.copyOf(out);
  }

  private static String norm(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }

  private static ZoneId parseZone(String tz) {
    try {
      return (tz == null || tz.isBlank()) ? ZoneId.of("UTC") : ZoneId.of(tz);
    } catch (Exception ex) {
      return ZoneId.of("UTC");
    }
  }

  private static LocalTime parseTime(String t) {
    try {
      return (t == null || t.isBlank()) ? null : LocalTime.parse(t);
    } catch (Exception ex) {
      return null;
    }
  }

  private static Set<DayOfWeek> parseDays(List<String> days) {
    if (days == null || days.isEmpty()) return Set.of();
    EnumSet<DayOfWeek> out = EnumSet.noneOf(DayOfWeek.class);
    for (String d : days) {
      if (d == null) continue;
      String x = d.trim().toUpperCase(Locale.ROOT);
      try {
        out.add(DayOfWeek.valueOf(x));
      } catch (Exception ignored) {
        // allow "MON" etc. if you ever use it
        switch (x) {
          case "MON" -> out.add(DayOfWeek.MONDAY);
          case "TUE" -> out.add(DayOfWeek.TUESDAY);
          case "WED" -> out.add(DayOfWeek.WEDNESDAY);
          case "THU" -> out.add(DayOfWeek.THURSDAY);
          case "FRI" -> out.add(DayOfWeek.FRIDAY);
          case "SAT" -> out.add(DayOfWeek.SATURDAY);
          case "SUN" -> out.add(DayOfWeek.SUNDAY);
        }
      }
    }
    return Collections.unmodifiableSet(out);
  }

  public record GameInfo(
      String providerKey, // "ny", "fl", "ga", "tx" (lower)
      String externalKey, // provider-specific key
      LocalTime drawTime, // from YAML if present
      ZoneId timezone, // from provider config
      boolean active,
      Set<DayOfWeek> days) {}
}
