package com.tchalanet.server.core.sales.api.command.result;

public record ReconcileTicketsForCorrectedDrawResultResult(
    int processedTickets,
    int updatedTickets,
    int skippedTickets
) {
}
