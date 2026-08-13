package com.tchalanet.server.features.pos.home.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PosHomeServiceReadinessTest {

  @Test
  void readinessIsReadyWhenSellerTerminalPresent() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var terminalId = SellerTerminalId.of(UUID.randomUUID());
    var service = new PosHomeService(null, null, null);

    var result = service.readiness(ctxWithTerminal(tenantId, terminalId));

    assertThat(result.ready()).isTrue();
    assertThat(result.blockers()).isEmpty();
  }

  @Test
  void readinessIsBlockedWhenNoSellerTerminal() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var service = new PosHomeService(null, null, null);

    var result = service.readiness(ctx(tenantId));

    assertThat(result.ready()).isFalse();
    assertThat(result.blockers()).hasSize(1);
    assertThat(result.blockers().getFirst().type()).isEqualTo("OPERATIONAL_CONTEXT");
  }

  @Test
  void readinessIsBlockedWhenContextNull() {
    var service = new PosHomeService(null, null, null);

    var result = service.readiness(null);

    assertThat(result.ready()).isFalse();
    assertThat(result.blockers()).hasSize(1);
  }

  private static TchRequestContext ctx(TenantId tenantId) {
    return new TchRequestContext(
        "tenant", tenantId.value(), "tenant", tenantId.value(),
        null, Set.of(), Set.of(), null, "request-id", null, null,
        false, null, "active", null, null, tenantId,
        ZoneId.of("America/Port-au-Prince"), null, null, null,
        null, Set.of(), Set.of(), null);
  }

  private static TchRequestContext ctxWithTerminal(TenantId tenantId, SellerTerminalId terminalId) {
    return new TchRequestContext(
        "tenant", tenantId.value(), "tenant", tenantId.value(),
        null, Set.of(), Set.of(), null, "request-id", null, null,
        false, null, "active", null, null, tenantId,
        ZoneId.of("America/Port-au-Prince"), null, null, null,
        terminalId, Set.of(), Set.of(), null);
  }
}
