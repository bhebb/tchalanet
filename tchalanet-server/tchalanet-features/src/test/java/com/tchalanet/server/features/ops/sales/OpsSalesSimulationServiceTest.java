package com.tchalanet.server.features.ops.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpsSalesSimulationServiceTest {

  private final OpsSalesSimulationService service =
      new OpsSalesSimulationService(new FailingCommandBus(), new FailingQueryBus());

  @Test
  void sellerContextKeepsSuperAdminActorAndInjectsSellerTerminal() {
    var tenantId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    var sellerTerminalId = UUID.fromString("20000000-0000-0000-0000-000000000001");

    var ctx = service.sellerContext(tenantId, sellerTerminalId, "HTG", "ops-sales-sim-test");

    assertThat(ctx.actorType()).isEqualTo(TchActorType.APP_USER);
    assertThat(ctx.sellerTerminalId()).isEqualTo(SellerTerminalId.of(sellerTerminalId));
    assertThat(ctx.systemRoles()).containsExactly(TchRole.SUPER_ADMIN);
    assertThat(ctx.roleCodes()).containsExactly(TchRole.SUPER_ADMIN.name());
    assertThat(ctx.permissionKeys()).containsExactly("admin.access");
  }

  @Test
  void idempotencyKeyFitsSalePreparationColumn() {
    var simulationId = UUID.fromString("30000000-0000-0000-0000-000000000001");

    var key = service.idempotencyKey(simulationId, 12345);

    assertThat(key).isEqualTo("ops-sales-sim:30000000-0000-0000-0000-000000000001:12345");
    assertThat(key).hasSizeLessThanOrEqualTo(96);
  }

  @Test
  void rejectsAnEmptyTicketMixWithTheDeclaredStableCode() {
    var exception =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () ->
                service.simulate(
                    request(new OpsSalesSimulationRequest.TicketMix(0, 0, 0, 0, 0), true, 1, null)),
            ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", "ops.sales_simulation.empty_mix");
  }

  @Test
  void rejectsExcessiveVolumeWithOnlyDeclaredPublicParameters() {
    var exception =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () ->
                service.simulate(
                    request(new OpsSalesSimulationRequest.TicketMix(2, 0, 0, 0, 0), true, 1, null)),
            ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", "ops.sales_simulation.too_many_tickets")
        .containsEntry("params", java.util.Map.of("requestedTickets", 2, "maxTickets", 1));
  }

  @Test
  void requiresAnAuditReasonBeforeExecutingSales() {
    var exception =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            () ->
                service.simulate(
                    request(
                        new OpsSalesSimulationRequest.TicketMix(1, 0, 0, 0, 0), false, 2, "  ")),
            ProblemRestException.class);

    assertThat(exception.getProblem().getProperties())
        .containsEntry("code", "ops.sales_simulation.reason_required");
  }

  private static OpsSalesSimulationRequest request(
      OpsSalesSimulationRequest.TicketMix mix, boolean dryRun, int maxTickets, String reason) {
    return new OpsSalesSimulationRequest(
        UUID.randomUUID(),
        List.of(UUID.randomUUID()),
        List.of(UUID.randomUUID()),
        mix,
        "HTG",
        BigDecimal.ONE,
        1L,
        dryRun,
        maxTickets,
        reason);
  }

  private static final class FailingCommandBus implements CommandBus {
    @Override
    public <R> R execute(Command<R> command) {
      throw new AssertionError("Command bus should not be called by this test");
    }
  }

  private static final class FailingQueryBus implements QueryBus {
    @Override
    public <R> R ask(Query<R> query) {
      throw new AssertionError("Query bus should not be called by this test");
    }
  }
}
