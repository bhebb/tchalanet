package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.api.query.GetPublicTicketVerificationRecordQuery;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyPayoutStatus;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyResponse;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketVerifyService {

  private static final int DEFAULT_VISIBILITY_DAYS = 14;

  private final QueryBus queryBus;
  private final TicketVerifyMapper mapper;

  public TicketVerifyResponse verify(String rawCode) {
    String code = normalize(rawCode);
    if (code.isBlank()) {
      return invalidCode(rawCode);
    }

    var record = queryBus.ask(new GetPublicTicketVerificationRecordQuery(code));
    if (record == null) {
      return notFound(code);
    }

    boolean expired = isExpired(record.createdAt(), DEFAULT_VISIBILITY_DAYS);
    return mapper.toResponse(record, expired);
  }

  private String normalize(String code) {
    if (code == null) return "";
    return code.trim().toUpperCase(java.util.Locale.ROOT).replace("-", "").replace(" ", "");
  }

  private boolean isExpired(Instant createdAt, int visibilityDays) {
    if (createdAt == null) return true;
    return createdAt.plus(Duration.ofDays(Math.max(1, visibilityDays))).isBefore(Instant.now());
  }

  private TicketVerifyResponse invalidCode(String raw) {
    return new TicketVerifyResponse(
        TicketVerifyStatus.INVALID_CODE, raw, TicketVerifyPayoutStatus.UNKNOWN,
        null, null, null, null, List.of());
  }

  private TicketVerifyResponse notFound(String code) {
    return new TicketVerifyResponse(
        TicketVerifyStatus.NOT_FOUND, code, TicketVerifyPayoutStatus.UNKNOWN,
        null, null, null, null, List.of());
  }
}
