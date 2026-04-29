package com.tchalanet.server.core.limitpolicy.domain.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.common.types.id.TestIds;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContextTest;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

@DisplayName("LimitResolver scoring")
class LimitResolverTest {

  private final LimitResolver resolver = new LimitResolver();

  @Test
  @DisplayName("should prefer terminal over agent for the same rule key")
  void shouldPreferTerminalOverAgentForSameRuleKey() {
    var ids = ids();
    var result =
        resolve(
            ids,
            assignment(ids.definitionId(), new LimitTarget.AgentTarget(ids.agentId()), "60"),
            assignment(ids.definitionId(), new LimitTarget.TerminalTarget(ids.terminalId()), "70"));

    assertThat(result.get(RuleKey.MAX_STAKE_PER_TICKET)).isEqualByComparingTo("70");
  }

  @Test
  @DisplayName("should prefer agent over outlet for the same rule key")
  void shouldPreferAgentOverOutletForSameRuleKey() {
    var ids = ids();
    var result =
        resolve(
            ids,
            assignment(ids.definitionId(), new LimitTarget.OutletTarget(ids.outletId()), "50"),
            assignment(ids.definitionId(), new LimitTarget.AgentTarget(ids.agentId()), "60"));

    assertThat(result.get(RuleKey.MAX_STAKE_PER_TICKET)).isEqualByComparingTo("60");
  }

  @Test
  @DisplayName("should prefer outlet over draw channel for the same rule key")
  void shouldPreferOutletOverDrawChannelForSameRuleKey() {
    var ids = ids();
    var result =
        resolve(
            ids,
            assignment(ids.definitionId(), new LimitTarget.DrawChannelTarget(ids.drawChannelId()), "40"),
            assignment(ids.definitionId(), new LimitTarget.OutletTarget(ids.outletId()), "50"));

    assertThat(result.get(RuleKey.MAX_STAKE_PER_TICKET)).isEqualByComparingTo("50");
  }

  @Test
  @DisplayName("should prefer draw channel over tenant for the same rule key")
  void shouldPreferDrawChannelOverTenantForSameRuleKey() {
    var ids = ids();
    var result =
        resolve(
            ids,
            assignment(ids.definitionId(), new LimitTarget.TenantTarget(), "10"),
            assignment(ids.definitionId(), new LimitTarget.DrawChannelTarget(ids.drawChannelId()), "40"));

    assertThat(result.get(RuleKey.MAX_STAKE_PER_TICKET)).isEqualByComparingTo("40");
  }

  @Test
  @DisplayName("should ignore non matching targets")
  void shouldIgnoreNonMatchingTargets() {
    var ids = ids();
    var otherTerminal = TestIds.terminal();
    var result =
        resolve(
            ids,
            assignment(ids.definitionId(), new LimitTarget.TenantTarget(), "10"),
            assignment(ids.definitionId(), new LimitTarget.TerminalTarget(otherTerminal), "70"));

    assertThat(result.get(RuleKey.MAX_STAKE_PER_TICKET)).isEqualByComparingTo("10");
  }

  private com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimits resolve(
      TestLimitIds ids, LimitAssignment... assignments) {
    var definition =
        new LimitDefinition(
            ids.definitionId(),
            RuleKey.MAX_STAKE_PER_TICKET,
            true,
            BreachOutcome.BLOCK,
            value("1"),
            null,
            null);
    var ctx =
        LimitContextTest.context(
            ids.tenantId(),
            ids.drawChannelId(),
            ids.agentId(),
            ids.terminalId(),
            ids.outletId());
    return resolver.resolve(List.of(definition), List.of(assignments), ctx);
  }

  private static LimitAssignment assignment(
      LimitDefinitionId definitionId, LimitTarget target, String value) {
    return new LimitAssignment(
        LimitAssignmentId.of(java.util.UUID.randomUUID()),
        definitionId,
        target,
        true,
        null,
        null,
        value(value),
        null,
        null);
  }

  private static JsonNode value(String value) {
    return JsonNodeFactory.instance.objectNode().put("value", new BigDecimal(value));
  }

  private static TestLimitIds ids() {
    return new TestLimitIds(
        TestIds.tenant(),
        TestIds.drawChannel(),
        TestIds.agent(),
        TestIds.terminal(),
        TestIds.outlet(),
        LimitDefinitionId.of(java.util.UUID.randomUUID()));
  }

  private record TestLimitIds(
      com.tchalanet.server.common.types.id.TenantId tenantId,
      com.tchalanet.server.common.types.id.DrawChannelId drawChannelId,
      com.tchalanet.server.common.types.id.AgentId agentId,
      com.tchalanet.server.common.types.id.TerminalId terminalId,
      com.tchalanet.server.common.types.id.OutletId outletId,
      LimitDefinitionId definitionId) {}
}
