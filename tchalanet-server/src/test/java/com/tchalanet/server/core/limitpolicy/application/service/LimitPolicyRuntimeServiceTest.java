package com.tchalanet.server.core.limitpolicy.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TestIds;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContextTest;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import com.tchalanet.server.core.limitpolicy.domain.resolver.LimitResolver;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LimitPolicyRuntimeService")
class LimitPolicyRuntimeServiceTest {

  @Test
  @DisplayName("should pass context targets to assignment reader when evaluating")
  void shouldPassContextTargetsToAssignmentReaderWhenEvaluating() {
    var tenantId = TestIds.tenant();
    var drawChannelId = TestIds.drawChannel();
    var agentId = TestIds.agent();
    var terminalId = TestIds.terminal();
    var outletId = TestIds.outlet();
    var context = LimitContextTest.context(tenantId, drawChannelId, agentId, terminalId, outletId);
    var assignments = new CapturingAssignmentReader();
    var service = new LimitPolicyRuntimeService(new EmptyDefinitionReader(), assignments, new LimitResolver());

    service.evaluate(context);

    assertThat(assignments.capturedTargets())
        .containsExactly(
            new LimitTarget.TenantTarget(),
            new LimitTarget.OutletTarget(outletId),
            new LimitTarget.AgentTarget(agentId),
            new LimitTarget.TerminalTarget(terminalId),
            new LimitTarget.DrawChannelTarget(drawChannelId));
  }

  private static final class EmptyDefinitionReader implements LimitDefinitionReaderPort {
    @Override
    public Optional<LimitDefinition> findById(LimitDefinitionId id) {
      return Optional.empty();
    }

    @Override
    public Optional<LimitDefinition> findByRuleKey(com.tchalanet.server.common.types.enums.RuleKey ruleKey) {
      return Optional.empty();
    }

    @Override
    public List<LimitDefinition> listActive() {
      return List.of();
    }
  }

  private static final class CapturingAssignmentReader implements LimitAssignmentReaderPort {
    private List<LimitTarget> capturedTargets = List.of();

    @Override
    public Optional<LimitAssignment> findById(LimitAssignmentId id) {
      return Optional.empty();
    }

    @Override
    public List<LimitAssignment> listByTarget(LimitTarget target) {
      return List.of();
    }

    @Override
    public Optional<LimitAssignment> findByNaturalKey(
        LimitTarget target, LimitDefinitionId definitionId) {
      return Optional.empty();
    }

    @Override
    public List<LimitAssignment> listActiveForTargets(List<LimitTarget> targets, Instant now) {
      capturedTargets = List.copyOf(targets);
      return List.of();
    }

    @Override
    public List<LimitAssignment> listActive(TenantId tenantId) {
      return LimitAssignmentReaderPort.super.listActive(tenantId);
    }

    List<LimitTarget> capturedTargets() {
      return capturedTargets;
    }
  }
}
