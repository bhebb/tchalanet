package com.tchalanet.server.core.sales.api.command.result;

public record RecordDrawTicketsResultResult(
    int processedTickets,
    int updatedTickets,
    int skippedTickets,
    long remainingTickets,
    int failedTickets) {

  public boolean complete() {
    return remainingTickets == 0 && failedTickets == 0;
  }
}
