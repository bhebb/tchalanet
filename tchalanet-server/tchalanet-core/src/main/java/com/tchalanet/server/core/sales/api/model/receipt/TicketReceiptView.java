package com.tchalanet.server.core.sales.api.model.receipt;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record TicketReceiptView(
    TicketId ticketId,
    String ticketCode,
    String displayCode,
    String publicCode,
    String verificationCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    String drawLabel,
    String outletName,
    String terminalCode,
    String sellerDisplayName,
    Instant placedAt,
    Locale locale,
    List<TicketReceiptGameSectionView> gameSections,
    String stakeTotal,
    String totalAmount,
    String potentialPayout,
    String verificationUrl
) {
    public TicketReceiptView {
        Objects.requireNonNull(ticketId, "ticketId is required");
        Objects.requireNonNull(ticketCode, "ticketCode is required");
        Objects.requireNonNull(displayCode, "displayCode is required");
        Objects.requireNonNull(publicCode, "publicCode is required");
        gameSections = List.copyOf(gameSections);
    }
}
