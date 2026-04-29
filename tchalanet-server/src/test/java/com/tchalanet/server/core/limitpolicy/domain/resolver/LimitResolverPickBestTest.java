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

@DisplayName("LimitResolver pick best")
class LimitResolverPickBestTest {

  private final LimitResolver resolver = new LimitResolver();

  @Test
  @DisplayName("should pick terminal assignment when terminal and agent target the same rule")
  void shouldPickTerminalAssignmentWhenTerminalAndAgentTargetSameRule() {
    var fixture = fixture();

    var limits =
        resolver.resolve(
            List.of(fixture.definition()),
            List.of(
                assignment(fixture.definition().id(), new LimitTarget.AgentTarget(fixture.agentId()), "60", true, null),
                assignment(fixture.definition().id(), new LimitTarget.TerminalTarget(fixture.terminalId()), "70", true, null)),
            fixture.context());

    assertThat(limits.get(RuleKey.MAX_STAKE_PER_TICKET)).isEqualByComparingTo("70");
  }

  @Test
  @DisplayName("should use tenant fallback when no specific assignment exists")
  void shouldUseTenantFallbackWhenNoSpecificAssignmentExists() {
    var fixture = fixture();

    var limits =
        resolver.resolve(
            List.of(fixture.definition()),
            List.of(assignment(fixture.definition().id(), new LimitTarget.TenantTarget(), "10", true, null)),
            fixture.context());

    assertThat(limits.get(RuleKey.MAX_STAKE_PER_TICKET)).isEqualByComparingTo("10");
  }

  @Test
  @DisplayName("should ignore inactive assignments")
  void shouldIgnoreInactiveAssignments() {
    var fixture = fixture();

    var limits =
        resolver.resolve(
            List.of(fixture.definition()),
            List.of(
                assignment(fixture.definition().id(), new LimitTarget.TenantTarget(), "10", true, null),
                assignment(fixture.definition().id(), new LimitTarget.TerminalTarget(fixture.terminalId()), "70", false, null)),
            fixture.context());

    assertThat(limits.get(RuleKey.MAX_STAKE_PER_TICKET)).isEqualByComparingTo("10");
  }

  private static LimitAssignment assignment(
      LimitDefinitionId definitionId,
      LimitTarget target,
      String value,
      boolean enabled,
      Instant deletedAt) {
    return new LimitAssignment(
        LimitAssignmentId.of(java.util.UUID.randomUUID()),
        definitionId,
        target,
        enabled,
        null,
        null,
        value(value),
        null,
        deletedAt);
  }

  private static JsonNode value(String value) {
    return JsonNodeFactory.instance.objectNode().put("value", new BigDecimal(value));
  }

  private static TestFixture fixture() {
    var tenantId = TestIds.tenant();
    var drawChannelId = TestIds.drawChannel();
    var agentId = TestIds.agent();
    var terminalId = TestIds.terminal();
    var outletId = TestIds.outlet();
    var definition =
        new LimitDefinition(
            LimitDefinitionId.of(java.util.UUID.randomUUID()),
            RuleKey.MAX_STAKE_PER_TICKET,
            true,
            BreachOutcome.BLOCK,
            value("1"),
            null,
            null);
    var context = LimitContextTest.context(tenantId, drawChannelId, agentId, terminalId, outletId);
    return new TestFixture(definition, context, agentId, terminalId);
  }

  private record TestFixture(
      LimitDefinition definition,
      com.tchalanet.server.core.limitpolicy.domain.model.LimitContext context,
      com.tchalanet.server.common.types.id.AgentId agentId,
      com.tchalanet.server.common.types.id.TerminalId terminalId) {}
}
