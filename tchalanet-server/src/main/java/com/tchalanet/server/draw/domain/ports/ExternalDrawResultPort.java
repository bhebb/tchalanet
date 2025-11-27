package com.tchalanet.server.draw.domain.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ExternalDrawResultPort {

  record DrawExternalQuery(UUID tenantId, String channelCode, LocalDate drawDate) {}

  record ExternalDrawResult(
      String channelCode, LocalDate drawDate, List<String> numbers, Map<String, Object> raw) {}

  Optional<ExternalDrawResult> fetchResult(DrawExternalQuery query);
}
