package com.tchalanet.server.features.ticketdelivery.app;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EdgeDeliveryPayload(
    UUID requestId,
    String channel,
    String recipient,
    String locale,
    boolean includePdf,
    boolean includeVerificationLink,
    String ticketCode,
    String publicCode,
    String verificationUrl,
    BigDecimal totalAmount,
    String currency,
    Instant soldAt,
    String outletName,
    String drawChannelLabel,
    String drawWhenLabel,
    List<EdgeDeliveryLine> lines
) {
  public record EdgeDeliveryLine(
      String gameCode,
      String selection,
      BigDecimal stake,
      BigDecimal potentialPayout
  ) {}
}
