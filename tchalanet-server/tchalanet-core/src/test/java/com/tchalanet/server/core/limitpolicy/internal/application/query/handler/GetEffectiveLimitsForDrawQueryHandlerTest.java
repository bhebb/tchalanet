package com.tchalanet.server.core.limitpolicy.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.api.query.GetEffectiveLimitsForDrawQuery;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.internal.domain.resolver.LimitResolver;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GetEffectiveLimitsForDrawQueryHandlerTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final Instant NOW = Instant.parse("2026-08-12T10:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final TenantId TENANT = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final DrawChannelId CHANNEL = DrawChannelId.of(UUID.fromString("30000000-0000-0000-0000-000000000001"));

  private final LimitAssignmentReaderPort assignments = mock(LimitAssignmentReaderPort.class);
  private final LimitResolver resolver = new LimitResolver();
  private final GetEffectiveLimitsForDrawQueryHandler handler =
      new GetEffectiveLimitsForDrawQueryHandler(assignments, resolver, FIXED);

  @Test
  void no_assignments_returns_empty_rules() {
    when(assignments.listActiveForTargets(any(), any())).thenReturn(List.of());

    var result = handler.handle(new GetEffectiveLimitsForDrawQuery(TENANT, CHANNEL));

    assertThat(result.rules()).isEmpty();
    assertThat(result.hasExposureLimit()).isFalse();
  }

  @Test
  void tenant_assignment_appears_with_TENANT_scope_name() {
    when(assignments.listActiveForTargets(any(), any()))
        .thenReturn(List.of(maxStakeAssignment(LimitScopeRef.tenant(TENANT))));

    var result = handler.handle(new GetEffectiveLimitsForDrawQuery(TENANT, CHANNEL));

    assertThat(result.rules()).hasSize(1);
    var rule = result.rules().get(0);
    assertThat(rule.ruleKey()).isEqualTo(RuleKey.MAX_STAKE_PER_LINE.name());
    assertThat(rule.resolvedScope()).isEqualTo("TENANT");
    assertThat(rule.limitConfigured()).isTrue();
  }

  @Test
  void draw_channel_assignment_appears_with_DRAW_CHANNEL_scope_name() {
    when(assignments.listActiveForTargets(any(), any()))
        .thenReturn(List.of(maxStakeAssignment(LimitScopeRef.drawChannel(CHANNEL))));

    var result = handler.handle(new GetEffectiveLimitsForDrawQuery(TENANT, CHANNEL));

    assertThat(result.rules()).hasSize(1);
    assertThat(result.rules().get(0).resolvedScope()).isEqualTo("DRAW_CHANNEL");
  }

  @Test
  void draw_channel_wins_over_tenant_for_same_rule() {
    // Both TENANT and DRAW_CHANNEL configured for MAX_STAKE_PER_LINE → DRAW_CHANNEL wins
    when(assignments.listActiveForTargets(any(), any()))
        .thenReturn(List.of(
            maxStakeAssignment(LimitScopeRef.tenant(TENANT)),
            maxStakeAssignment(LimitScopeRef.drawChannel(CHANNEL))));

    var result = handler.handle(new GetEffectiveLimitsForDrawQuery(TENANT, CHANNEL));

    // resolver collapses to 1 rule; most specific (DRAW_CHANNEL) wins
    assertThat(result.rules()).hasSize(1);
    assertThat(result.rules().get(0).resolvedScope()).isEqualTo("DRAW_CHANNEL");
  }

  @Test
  void exposure_limit_rule_sets_hasExposureLimit_true() {
    var exposureAssignment = LimitAssignment.createNew(
        LimitAssignmentId.of(UUID.randomUUID()),
        RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,
        LimitScopeRef.drawChannel(CHANNEL),
        true, BreachOutcome.BLOCK,
        MAPPER.createObjectNode().put("valueCents", 100000L),
        null, null);

    when(assignments.listActiveForTargets(any(), any())).thenReturn(List.of(exposureAssignment));

    var result = handler.handle(new GetEffectiveLimitsForDrawQuery(TENANT, CHANNEL));

    assertThat(result.hasExposureLimit()).isTrue();
  }

  private static LimitAssignment maxStakeAssignment(LimitScopeRef scope) {
    return LimitAssignment.createNew(
        LimitAssignmentId.of(UUID.randomUUID()),
        RuleKey.MAX_STAKE_PER_LINE,
        scope,
        true, BreachOutcome.BLOCK,
        MAPPER.createObjectNode().put("valueCents", 10000L),
        null, null);
  }
}
