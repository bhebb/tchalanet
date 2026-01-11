package com.tchalanet.server.core.draw.application.print;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ResultSlotLabelResolver {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  public String resolve(String slotKey, LocalTime drawTime, Locale locale) {
    if (slotKey == null || slotKey.isBlank()) return "-";

    // slotKey format: "NY_MID"
    String[] parts = slotKey.trim().toUpperCase(Locale.ROOT).split("_", 2);
    String provider = parts.length > 0 ? parts[0] : slotKey;
    String slot = parts.length > 1 ? parts[1] : "";

    String providerLabel = providerLabel(provider, locale);
    String slotLabel = slotLabel(slot, locale);

    String time = (drawTime == null) ? null : drawTime.format(TIME);

    if (time == null || time.isBlank()) return providerLabel + " • " + slotLabel;
    return providerLabel + " • " + slotLabel + " • " + time;
  }

  private String providerLabel(String p, Locale locale) {
    return switch (p) {
      case "NY" -> "New York";
      case "FL" -> "Florida";
      case "GA" -> "Georgia";
      case "TX" -> "Texas";
      default -> p;
    };
  }

  private String slotLabel(String s, Locale locale) {
    return switch (s) {
      case "MID" -> "Midday";
      case "EVE" -> "Evening";
      default -> s;
    };
  }
}
