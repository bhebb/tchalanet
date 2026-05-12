package com.tchalanet.server.core.limitpolicy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.TestIds;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LimitContext")
public class LimitContextTest {

  @Test
  @DisplayName("should expose all targets when all IDs are present")
  void shouldExposeAllTargetsWhenAllIdsArePresent() {
    var tenantId = TestIds.tenant();
    var drawChannelId = TestIds.drawChannel();
    var agentId = TestIds.agent();
    var terminalId = TestIds.terminal();
    var outletId = TestIds.outlet();

    var targets =
        context(tenantId, drawChannelId, agentId, terminalId, outletId).toTargets();

    assertThat(targets)
        .containsExactly(
            new LimitTarget.TenantTarget(),
            new LimitTarget.OutletTarget(outletId),
            new LimitTarget.AgentTarget(agentId),
            new LimitTarget.TerminalTarget(terminalId),
            new LimitTarget.DrawChannelTarget(drawChannelId));
  }

  @Test
  @DisplayName("should expose only tenant and agent targets when optional IDs are absent")
  void shouldExposePartialTargetsWhenOnlyAgentIsPresent() {
    var tenantId = TestIds.tenant();
    var agentId = TestIds.agent();

    var targets = context(tenantId, null, agentId, null, null).toTargets();

    assertThat(targets)
        .containsExactly(new LimitTarget.TenantTarget(), new LimitTarget.AgentTarget(agentId));
  }

  public static LimitContext context(
      com.tchalanet.server.common.types.id.TenantId tenantId,
      com.tchalanet.server.common.types.id.DrawChannelId drawChannelId,
      com.tchalanet.server.common.types.id.AgentId agentId,
      com.tchalanet.server.common.types.id.TerminalId terminalId,
      com.tchalanet.server.common.types.id.OutletId outletId) {
    return new LimitContext(
        tenantId,
        TestIds.draw(),
        drawChannelId,
        agentId,
        terminalId,
        outletId,
        null,
        List.of(),
        null,
        OperationType.SALE,
        new LimitScopeRef.TenantScope(tenantId),
        List.of(),
        BigDecimal.ZERO,
        0,
        Instant.parse("2026-04-28T00:00:00Z"),
        ZoneId.of("America/Toronto"));
  }
}
