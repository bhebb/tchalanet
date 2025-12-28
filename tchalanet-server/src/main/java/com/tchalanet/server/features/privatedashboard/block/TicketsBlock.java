package com.tchalanet.server.features.privatedashboard.block;

import java.util.List;

public record TicketsBlock(int openTickets, int closedTickets, List<String> recentTicketIds) {
  public static TicketsBlock empty() {
    return new TicketsBlock(0, 0, List.of());
  }
}
