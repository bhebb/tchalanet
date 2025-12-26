package com.tchalanet.server.core.sales.application.command.model;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecordTicketResultCommand(TenantId tenantId, TicketId ticketId, BigDecimal winningAmount, Instant resultedAt) implements Command<Ticket> {
}
