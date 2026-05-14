package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.TicketSettlementStatus;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Internal record returned by core.sales for public ticket verification.
 * tenantId is internal — must never be exposed in the public response.
 */
public record PublicTicketVerificationRecord(
    TenantId tenantId,
    String publicCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    Instant createdAt,
    BigDecimal totalAmount,
    BigDecimal winningAmount,
    String outletName,
    String outletCity,
    String outletCountry,
    List<PublicTicketVerificationLineRecord> lines
) {}
