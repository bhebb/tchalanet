package com.tchalanet.server.core.sales.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.status.TicketPrintStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.time.Instant;

public record TicketResponse(
    TicketId id,
    String ticketCode,
    String publicCode,
    String verificationCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    TicketSaleChannel saleChannel,
    DrawId drawId,
    SellerTerminalId sellerTerminalId,
    Money totalAmount,
    Money potentialPayoutAmount,
    TicketPrintStatus printStatus,
    Instant soldAt,
    Instant placedAt
) {}
