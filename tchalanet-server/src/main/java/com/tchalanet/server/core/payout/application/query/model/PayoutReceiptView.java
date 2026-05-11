package com.tchalanet.server.core.payout.application.query.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import java.math.BigDecimal;
import java.time.Instant;

public record PayoutReceiptView(
    TenantId tenantId,
    PayoutId payoutId,
    TicketId ticketId,
    BigDecimal amount,
    String currency,
    Instant paidAt,
    OutletId payingOutletId,
    SalesSessionId payingSessionId,
    TerminalId terminalId,
    String paidByLabel) {}
