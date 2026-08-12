package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitLineContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MaxStakePerSelectionPerTicketEvaluatorTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final LimitScopeRef SCOPE = LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));
  private static final LimitFactsSnapshot EMPTY_FACTS = new LimitFactsSnapshot(Map.of());

  private final MaxStakePerSelectionPerTicketEvaluator evaluator =
      new MaxStakePerSelectionPerTicketEvaluator();

  @Test
  void supports_max_stake_per_selection_per_ticket() {
    assertThat(evaluator.supports()).isEqualTo(RuleKey.MAX_STAKE_PER_SELECTION_PER_TICKET);
  }

  @Test
  void single_line_below_limit_does_not_breach() {
    var breaches = evaluate(1000, line("12", 800));
    assertThat(breaches).isEmpty();
  }

  @Test
  void single_line_exactly_at_limit_does_not_breach() {
    var breaches = evaluate(1000, line("12", 1000));
    assertThat(breaches).isEmpty();
  }

  @Test
  void single_line_above_limit_breaches() {
    var breaches = evaluate(1000, line("12", 1500));

    assertThat(breaches).hasSize(1);
    assertThat(breaches.get(0).ruleKey()).isEqualTo(RuleKey.MAX_STAKE_PER_SELECTION_PER_TICKET);
    assertThat(breaches.get(0).limitValue()).isEqualTo(1000L);
    assertThat(breaches.get(0).deltaValue()).isEqualTo(1500L);
  }

  @Test
  void multiple_lines_same_selection_aggregate_before_comparing() {
    // 600 + 600 = 1200 > 1000 → breach
    var breaches = evaluate(1000, line("12", 600), line("12", 600));
    assertThat(breaches).hasSize(1);
    assertThat(breaches.get(0).deltaValue()).isEqualTo(1200L);
  }

  @Test
  void different_selections_evaluated_independently() {
    // "12" = 800 (ok), "34" = 1200 (breach)
    var breaches = evaluate(1000, line("12", 800), line("34", 1200));
    assertThat(breaches).hasSize(1);
    assertThat(breaches.get(0).deltaValue()).isEqualTo(1200L);
  }

  @Test
  void same_selection_different_bet_type_not_aggregated() {
    // BetType is part of the selection key → separate evaluations
    var breaches =
        evaluate(
            1000,
            new LimitLineContext(BetType.MATCH_1_2D, "12", 600),
            new LimitLineContext(BetType.MATCH_2_2D, "12", 600));
    // 600 per each (BetType,selection) pair → neither breaches
    assertThat(breaches).isEmpty();
  }

  @Test
  void no_lines_produces_no_breaches() {
    var breaches = evaluate(1000);
    assertThat(breaches).isEmpty();
  }

  private List<LimitBreach> evaluate(long valueCents, LimitLineContext... lines) {
    var params = MAPPER.createObjectNode().put("valueCents", valueCents);
    var rule =
        new EffectiveLimitRule(
            RuleKey.MAX_STAKE_PER_SELECTION_PER_TICKET, BreachOutcome.BLOCK, SCOPE, null, params);
    var ctx =
        new LimitContext(
            TenantId.of(UUID.randomUUID()), null, null, null, null, Instant.now(), List.of(lines));
    return evaluator.evaluate(rule, EMPTY_FACTS, ctx);
  }

  private static LimitLineContext line(String selection, long stakeCents) {
    return new LimitLineContext(BetType.MATCH_1_2D, selection, stakeCents);
  }
}
