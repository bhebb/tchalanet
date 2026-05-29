package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;

public record TicketPrintIdentity(
    TicketId ticketId,
    TenantId tenantId,
    String ticketCode,
    String publicCode,
    String verificationCode
) {
}
