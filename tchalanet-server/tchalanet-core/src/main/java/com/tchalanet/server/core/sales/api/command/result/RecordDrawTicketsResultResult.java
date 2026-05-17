package com.tchalanet.server.core.sales.api.command.result;

public record RecordDrawTicketsResultResult(
    int processedTickets,
    int updatedTickets,
    int skippedTickets
) {
}
