package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.common.types.money.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TicketVerificationProjection(
    String publicCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    Instant placedAt,
    Money totalAmount,
    Money winningAmount,
    DrawProjection draw,
    OutletProjection outlet,
    List<LineProjection> lines
) {
    public record DrawProjection(
        String drawChannelName,
        LocalDate drawDate,
        Instant scheduledAt
    ) {}

    public record OutletProjection(
        String outletName
    ) {}

    public record LineProjection(
        int lineNumber,
        GameCode gameCode,
        BetType betType,
        String displaySelection,
        Money stake,
        Money potentialPayout
    ) {}
}
