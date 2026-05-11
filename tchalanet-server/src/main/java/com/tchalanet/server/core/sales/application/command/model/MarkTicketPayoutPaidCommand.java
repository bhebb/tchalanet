package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

public record MarkTicketPayoutPaidCommand(TenantId tenantId, TicketId ticketId, UserId userId, String payoutPaid, String currency) {

}
