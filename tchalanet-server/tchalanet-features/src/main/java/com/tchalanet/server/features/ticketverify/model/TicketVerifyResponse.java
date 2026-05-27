package com.tchalanet.server.features.ticketverify.model;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Public ticket verification response. Must never expose internal IDs
 * (ticketId, drawId, tenantId, id, addressId, etc.).
 */
public record TicketVerifyResponse(
    String publicCode,
    String displayCode,
    CustomerTicketStatus status,
    Money totalAmount,
    Money winningAmount,
    Instant placedAt,
    TicketVerifyOutletView outlet,
    DrawView draw,
    List<LineView> lines
) {
    public record DrawView(
        String channelName,
        String channelLabel,
        LocalDate drawDate,
        Instant scheduledAt
    ) {}

    public record LineView(
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
