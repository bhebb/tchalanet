package com.tchalanet.server.core.limitpolicy.internal.domain.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitAssignment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LimitResolverTest {

  private static final TenantId TENANT = TenantId.of(UUID.randomUUID());
  private static final DrawChannelId CHANNEL = DrawChannelId.of(UUID.randomUUID());
  private static final SellerTerminalId TERMINAL = SellerTerminalId.of(UUID.randomUUID());
  private static final Instant NOW = Instant.parse("2026-08-12T10:00:00Z");

  private final LimitResolver resolver = new LimitResolver();

  @Test
  void sellerTerminalOverridesDrawChannelOverridesTenant() {
    var assignments =
        List.of(
            assignment(LimitScopeRef.tenant(TENANT), 500),
            assignment(LimitScopeRef.drawChannel(CHANNEL), 300),
            assignment(LimitScopeRef.sellerTerminal(TERMINAL), 100));

    var ctx = ctx(true);
    var result = resolver.resolve(assignments, ctx);

    var rule = result.rules().get(RuleKey.MAX_STAKE_PER_LINE);
    assertThat(rule).isNotNull();
    assertThat(rule.appliedScope()).isInstanceOf(LimitScopeRef.SellerTerminalScope.class);
  }

  @Test
  void drawChannelOverridesTenantWhenNoTerminalOverride() {
    var assignments =
        List.of(
            assignment(LimitScopeRef.tenant(TENANT), 500),
            assignment(LimitScopeRef.drawChannel(CHANNEL), 300));

    var ctx = ctx(true);
    var result = resolver.resolve(assignments, ctx);

    var rule = result.rules().get(RuleKey.MAX_STAKE_PER_LINE);
    assertThat(rule).isNotNull();
    assertThat(rule.appliedScope()).isInstanceOf(LimitScopeRef.DrawChannelScope.class);
  }

  @Test
  void tenantFallsBackWhenNoOtherOverride() {
    var assignments = List.of(assignment(LimitScopeRef.tenant(TENANT), 500));

    var ctx = ctx(true);
    var result = resolver.resolve(assignments, ctx);

    var rule = result.rules().get(RuleKey.MAX_STAKE_PER_LINE);
    assertThat(rule).isNotNull();
    assertThat(rule.appliedScope()).isInstanceOf(LimitScopeRef.TenantScope.class);
  }

  @Test
  void nullResultWhenNoAssignmentAtAnyScope() {
    var ctx = ctx(true);
    var result = resolver.resolve(List.of(), ctx);

    assertThat(result.rules()).isEmpty();
    assertThat(result.rules().get(RuleKey.MAX_STAKE_PER_LINE)).isNull();
  }

  @Test
  void sellerTerminalNotMatchedWhenContextHasNoTerminal() {
    var assignments =
        List.of(
            assignment(LimitScopeRef.tenant(TENANT), 500),
            assignment(LimitScopeRef.sellerTerminal(TERMINAL), 100));

    // ctx without sellerTerminalId
    var ctx = ctx(false);
    var result = resolver.resolve(assignments, ctx);

    // SELLER_TERMINAL score is -1 (not applicable) — only TENANT wins
    var rule = result.rules().get(RuleKey.MAX_STAKE_PER_LINE);
    assertThat(rule).isNotNull();
    assertThat(rule.appliedScope()).isInstanceOf(LimitScopeRef.TenantScope.class);
  }

  private static LimitContext ctx(boolean withTerminal) {
    return new LimitContext(
        TENANT, null, withTerminal ? TERMINAL : null, null, CHANNEL, NOW, List.of());
  }

  private static LimitAssignment assignment(LimitScopeRef scope, int unused) {
    return LimitAssignment.createNew(
        LimitAssignmentId.of(UUID.randomUUID()),
        RuleKey.MAX_STAKE_PER_LINE,
        scope,
        true,
        BreachOutcome.BLOCK,
        null,
        null,
        null);
  }
}
