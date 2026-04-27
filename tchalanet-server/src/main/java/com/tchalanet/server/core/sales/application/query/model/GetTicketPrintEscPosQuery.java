package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;
import jakarta.validation.constraints.NotNull;

public record GetTicketPrintEscPosQuery(@NotNull TicketId ticketId) implements Query<byte[]> {}
