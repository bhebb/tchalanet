package com.tchalanet.server.core.sales.application.query;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;

public record GetTicketPrintEscPosQuery(UUID ticketId) implements Query<byte[]> {}

