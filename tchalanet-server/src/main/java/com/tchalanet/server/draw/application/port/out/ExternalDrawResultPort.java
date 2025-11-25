package com.tchalanet.server.draw.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ExternalDrawResultPort {
  Optional<ExternalDrawResult> fetchResult(DrawExternalQuery query);

  record DrawExternalQuery(UUID tenantId, String channelCode, LocalDate drawDate) {}

  record ExternalDrawResult(
      String channelCode, LocalDate drawDate, List<String> numbers, Map<String, Object> extra) {}
}
