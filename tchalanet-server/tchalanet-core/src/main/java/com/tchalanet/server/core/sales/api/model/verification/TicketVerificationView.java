package com.tchalanet.server.core.sales.api.model.verification;

import com.tchalanet.server.common.types.money.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TicketVerificationView(
    String publicCode,
    String displayCode,
    CustomerTicketStatus status,
    Money totalAmount,
    Money winningAmount,
    Instant placedAt,
    DrawInfoView draw,
    OutletInfoView outlet,
    List<TicketLineView> lines
) {
    public record DrawInfoView(
        String channelKey,
        String channelName,
        LocalDate drawDate,
        Instant scheduledAt
    ) {}

    public record OutletInfoView(
        String name
    ) {}

    public record TicketLineView(
        int lineNumber,
        String gameDisplayName,
        String betTypeLabel,
        String optionLabel,
        String selection,
        Money stake,
        Money potentialPayout,
        boolean promotional,
        String promotionLabel
    ) {}
}
