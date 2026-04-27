package com.tchalanet.server.common.config.batch;

import jakarta.annotation.PostConstruct;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.batch")
@Getter
@Setter
public class BatchWindowsConfig {

  /** Exemple: "12:00-14:00,20:00-23:00" */
  private String fetchResultsWindows;

  private String settleDrawsWindows;
  private String closeDrawsWindows;

  /** Fenêtre par défaut pour l'ouverture des tirages (ops/scheduler) */
  private String openDrawsWindows = "02:00-06:00";

  private List<TimeRange> closeRanges = List.of();
  private List<TimeRange> fetchRanges = List.of();
  private List<TimeRange> settleRanges = List.of();
  private List<TimeRange> openRanges = List.of();

  @PostConstruct
  void init() {
    this.fetchRanges = parse(fetchResultsWindows);
    this.settleRanges = parse(settleDrawsWindows);
    this.closeRanges = parse(closeDrawsWindows);
    this.openRanges = parse(openDrawsWindows);
  }

  public boolean isInFetchResultsWindow(LocalTime now) {
    return fetchRanges.stream().anyMatch(r -> r.contains(now));
  }

  public boolean isInSettleDrawsWindow(LocalTime now) {
    return settleRanges.stream().anyMatch(r -> r.contains(now));
  }

  public boolean isInCloseDrawsWindow(LocalTime now) {
    return closeRanges.stream().anyMatch(r -> r.contains(now));
  }

  public boolean isInOpenDrawsWindow(LocalTime now) {
    return openRanges.stream().anyMatch(r -> r.contains(now));
  }

  private List<TimeRange> parse(String value) {
    if (value == null || value.isBlank()) return List.of();
    return Arrays.stream(value.split(",")).map(String::trim).map(this::toRange).toList();
  }

  private TimeRange toRange(String token) {
    String[] parts = token.split("-");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid time range: " + token);
    }
    return new TimeRange(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]));
  }

  public record TimeRange(LocalTime from, LocalTime to) {
    boolean contains(LocalTime t) {
      // support ranges that cross midnight, e.g. 23:00-02:00
      if (from.equals(to)) {
        // full day
        return true;
      }
      if (!from.isAfter(to)) {
        // normal range
        return !t.isBefore(from) && !t.isAfter(to);
      }
      // wraps midnight
      return !t.isBefore(from) || !t.isAfter(to);
    }
  }
}
