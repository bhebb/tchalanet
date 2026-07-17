package com.tchalanet.server.features.ops.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OpsSalesSimulationRequest(
    @NotNull UUID tenantId,
    @NotEmpty List<UUID> drawIds,
    @NotEmpty List<UUID> sellerTerminalIds,
    @Valid TicketMix ticketMix,
    String currency,
    @DecimalMin(value = "0.01") BigDecimal stakeAmount,
    Long seed,
    Boolean dryRun,
    Integer maxTickets,
    String reason) {

  public record TicketMix(
      Integer bolet, Integer maryaj, Integer loto3, Integer loto4, Integer loto5) {
    int count(SimulatedGameKind kind) {
      return switch (kind) {
        case BOLET -> positive(bolet);
        case MARYAJ -> positive(maryaj);
        case LOTO3 -> positive(loto3);
        case LOTO4 -> positive(loto4);
        case LOTO5 -> positive(loto5);
      };
    }

    int total() {
      int total = 0;
      for (var kind : SimulatedGameKind.values()) {
        total += count(kind);
      }
      return total;
    }

    private static int positive(Integer value) {
      return value == null || value < 0 ? 0 : value;
    }
  }
}
