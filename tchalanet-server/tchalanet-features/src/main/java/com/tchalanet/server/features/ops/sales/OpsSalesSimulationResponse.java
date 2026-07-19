package com.tchalanet.server.features.ops.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OpsSalesSimulationResponse(
    UUID simulationId,
    UUID tenantId,
    boolean dryRun,
    String currency,
    BigDecimal stakeAmount,
    long seed,
    int requestedTickets,
    int plannedTickets,
    int preparedTickets,
    int confirmedTickets,
    int rejectedTickets,
    int failedTickets,
    BigDecimal confirmedGross,
    List<DrawSummary> draws,
    List<SellerSummary> sellers,
    List<GameSummary> games,
    List<TicketResult> tickets,
    List<TicketFailure> failures,
    List<String> nextActions) {

  public record DrawSummary(
      UUID drawId,
      String channelCode,
      String status,
      int planned,
      int confirmed,
      int rejected,
      int failed) {}

  public record SellerSummary(
      UUID sellerTerminalId,
      String terminalCode,
      String displayName,
      int planned,
      int confirmed,
      int rejected,
      int failed) {}

  public record GameSummary(
      String game,
      String gameCode,
      String betType,
      Short betOption,
      int planned,
      int confirmed,
      int rejected,
      int failed) {}

  public record TicketResult(
      UUID drawId,
      UUID sellerTerminalId,
      String game,
      String selection,
      UUID preparationId,
      UUID ticketId,
      String publicCode,
      BigDecimal amount) {}

  public record TicketFailure(
      UUID drawId,
      UUID sellerTerminalId,
      String game,
      String selection,
      Integer status,
      String message) {}
}
