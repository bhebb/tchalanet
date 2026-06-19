package com.tchalanet.server.core.sales.api.model.receipt;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.tchalanet.server.common.types.money.Money;

public record TicketReceiptView(
    TicketId ticketId,
    TenantId tenantId,
    String ticketCode,
    String displayCode,
    String publicCode,
    String verificationCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    String tenantDisplayName,
    String tenantReceiptHeader,
    String outletReceiptHeader,
    // drawLabel: optional localized slot alias (may be null)
    String drawLabel,
    // drawChannelLabel: preferred market/channel name (e.g. "Texas")
    String drawChannelLabel,
    Instant drawScheduledAt,
    String terminalCode,
    String sellerDisplayName,
    Instant placedAt,
    Locale locale,
    ZoneId timezone,
    List<TicketReceiptGameSectionView> gameSections,
    Money stakeTotal,
    Money totalAmount,
    Money potentialPayout,
    String tenantReceiptFooter,
    String verificationUrl,
    // false = first print (ORIGINAL), true = any subsequent print (DUPLICATA)
    boolean isReprint
) {
    public TicketReceiptView {
        Objects.requireNonNull(ticketId, "ticketId is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(ticketCode, "ticketCode is required");
        Objects.requireNonNull(displayCode, "displayCode is required");
        Objects.requireNonNull(publicCode, "publicCode is required");
        gameSections = List.copyOf(gameSections);
    }
}
