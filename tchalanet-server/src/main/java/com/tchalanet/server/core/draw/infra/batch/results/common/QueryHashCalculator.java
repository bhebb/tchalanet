package com.tchalanet.server.core.draw.infra.batch.results.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;

public final class QueryHashCalculator {

  private QueryHashCalculator() {}

  public static String stableHash(
      String provider,
      LocalDate drawDate,
      List<String> channelCodes,
      int maxDraws,
      String queryShape) {

    String canonical =
        "provider="
            + (provider == null ? "" : provider.trim())
            + "|draw_date="
            + (drawDate == null ? "" : drawDate)
            + "|channel_codes="
            + String.join(",", channelCodes == null ? List.of() : channelCodes)
            + "|max_draws="
            + maxDraws
            + "|query="
            + (queryShape == null ? "" : queryShape.trim());

    return sha256Hex(canonical);
  }

  public static String sha256Hex(String s) {
    try {
      var md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
      var sb = new StringBuilder(dig.length * 2);
      for (byte b : dig) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("sha256 failed", e);
    }
  }
}
