package com.tchalanet.server.draw.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExternalDrawResultPort {

  ExternalDrawResult fetchExternalResult(UUID tenantId, UUID drawId);

  record DrawExternalQuery(UUID tenantId, String channelCode, LocalDate drawDate) {}

  record ExternalDrawResult(
      String channelCode,
      LocalDate drawDate,
      List<String> numbers,
      List<String> numbersExtra,
      Instant occurredAt,
      String rawPayload) {}
}
