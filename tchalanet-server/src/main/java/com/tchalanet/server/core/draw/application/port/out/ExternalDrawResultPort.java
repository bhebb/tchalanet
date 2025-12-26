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
      Map<String, Object> rawPayload,
      Instant occurredAt,
      List<String> numbersExtra) {

    public static ExternalDrawResult notFound(String status, Map<String, Object> rawPayload) {
      return new ExternalDrawResult(false, status, List.of(), rawPayload, null, List.of());
    }

    public static ExternalDrawResult found(
        String status,
        List<String> numbers,
        List<String> numbersExtra,
        Instant occurredAt,
        Map<String, Object> rawPayload) {
      return new ExternalDrawResult(true, status, numbers, rawPayload, occurredAt, numbersExtra);
    }
  }
}
