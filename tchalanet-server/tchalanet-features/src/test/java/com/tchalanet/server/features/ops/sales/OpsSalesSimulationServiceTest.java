package com.tchalanet.server.features.ops.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.SellerTerminalId;
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
