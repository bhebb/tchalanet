package com.tchalanet.server.core.draw.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExternalDrawResultPort {

  ExternalDrawResult fetchExternalResult(DrawExternalQuery query);

  /** Query basée sur un SLOT (channel + date locale). */
  record DrawExternalQuery(
      String channelCode, LocalDate drawDateLocal, Instant executedAt, boolean force) {
    public DrawExternalQuery {
      if (channelCode == null || channelCode.isBlank())
        throw new IllegalArgumentException("channelCode required");
      if (drawDateLocal == null) throw new IllegalArgumentException("drawDateLocal required");
      if (executedAt == null) throw new IllegalArgumentException("executedAt required");
    }
  }

  record ExternalDrawResult(
      boolean found,
      String status,
      List<String> numbers,
      List<String> numbersExtra,
      Instant occurredAt,
      com.tchalanet.server.common.types.enums.ResultQuality quality,
      Map<String, Object> rawPayload) {

    public static ExternalDrawResult notFound(String status, Map<String, Object> raw) {
      return new ExternalDrawResult(false, status, List.of(), List.of(), null, com.tchalanet.server.common.types.enums.ResultQuality.SUSPECT, raw);
    }

    public static ExternalDrawResult found(
        String status,
        List<String> main,
        List<String> extra,
        Instant occurredAt,
        com.tchalanet.server.common.types.enums.ResultQuality quality,
        Map<String, Object> raw) {
      return new ExternalDrawResult(true, status, main, extra, occurredAt, quality, raw);
    }
  }

  // Bulk query: multiple channelCodes for same drawDate -> map channelCode -> result
  record DrawExternalBulkQuery(
      List<String> channelCodes,
      LocalDate drawDateLocal,
      Instant executedAtUtc,
      boolean force,
      boolean dryRun,
      int maxDraws) {
    public DrawExternalBulkQuery {
      if (channelCodes == null) throw new IllegalArgumentException("channelCodes required");
      if (drawDateLocal == null) throw new IllegalArgumentException("drawDateLocal required");
      if (executedAtUtc == null) throw new IllegalArgumentException("executedAtUtc required");
    }
  }

  /**
   * Retourne une map: channelCode -> résultat (si trouvé).
   */
  Map<String, ExternalDrawResult> fetchExternalResults(DrawExternalBulkQuery query);
}
