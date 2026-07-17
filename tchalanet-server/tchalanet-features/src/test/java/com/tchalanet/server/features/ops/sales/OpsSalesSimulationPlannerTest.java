package com.tchalanet.server.features.ops.sales;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpsSalesSimulationPlannerTest {

  private final OpsSalesSimulationPlanner planner = new OpsSalesSimulationPlanner();

  @Test
  void buildsDeterministicPlanForMultipleDrawsAndSellers() {
    var draw1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
    var draw2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
    var seller1 = UUID.fromString("20000000-0000-0000-0000-000000000001");
    var seller2 = UUID.fromString("20000000-0000-0000-0000-000000000002");
    var request =
        new OpsSalesSimulationRequest(
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            List.of(draw1, draw2),
            List.of(seller1, seller2),
            new OpsSalesSimulationRequest.TicketMix(2, 1, 1, 1, 1),
            "HTG",
            new BigDecimal("1.00"),
            42L,
            true,
            100,
            "qa");

    var first = planner.plan(request, new BigDecimal("1.00"), 42L);
    var second = planner.plan(request, new BigDecimal("1.00"), 42L);

    assertThat(planner.requestedTickets(request)).isEqualTo(24);
    assertThat(first).hasSize(24);
    assertThat(first).extracting(OpsSalesSimulationPlanner.PlannedTicket::selection)
        .containsExactlyElementsOf(
            second.stream().map(OpsSalesSimulationPlanner.PlannedTicket::selection).toList());
    assertThat(first.stream().filter(t -> t.kind() == SimulatedGameKind.BOLET)).hasSize(8);
    assertThat(first.stream().filter(t -> t.kind() == SimulatedGameKind.MARYAJ)).hasSize(4);
  }

  @Test
  void negativeMixCountsAreIgnored() {
    var request =
        new OpsSalesSimulationRequest(
            UUID.randomUUID(),
            List.of(UUID.randomUUID()),
            List.of(UUID.randomUUID()),
            new OpsSalesSimulationRequest.TicketMix(-1, null, 1, 0, 0),
            "HTG",
            BigDecimal.ONE,
            7L,
            true,
            100,
            null);

    assertThat(planner.requestedTickets(request)).isEqualTo(1);
    assertThat(planner.plan(request, BigDecimal.ONE, 7L))
        .singleElement()
        .satisfies(ticket -> assertThat(ticket.kind()).isEqualTo(SimulatedGameKind.LOTO3));
  }
}
