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

class MaxSalesCountPerSelectionPerDrawEvaluatorTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final LimitScopeRef SCOPE = LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));

  private final MaxSalesCountPerSelectionPerDrawEvaluator evaluator =
      new MaxSalesCountPerSelectionPerDrawEvaluator();

  @Test
  void supports_max_sales_count_per_selection_per_draw() {
    assertThat(evaluator.supports()).isEqualTo(RuleKey.MAX_SALES_COUNT_PER_SELECTION_PER_DRAW);
  }

  @Test
  void first_sale_on_selection_does_not_breach_below_limit() {
    var breaches = evaluate(5, emptyFacts(), line("12"));
    assertThat(breaches).isEmpty();
  }

  @Test
  void exactly_at_count_limit_does_not_breach() {
    // current=4, delta=1, next=5, limit=5: 5 > 5 is false
    var facts = factsWithCount(SCOPE, 4);
    var breaches = evaluate(5, facts, line("12"));
    assertThat(breaches).isEmpty();
  }

  @Test
  void exceeding_limit_breaches_with_sales_count_reported() {
    var facts = factsWithCount(SCOPE, 5);
    var breaches = evaluate(5, facts, line("12"));

    assertThat(breaches).hasSize(1);
    var breach = breaches.get(0);
    assertThat(breach.ruleKey()).isEqualTo(RuleKey.MAX_SALES_COUNT_PER_SELECTION_PER_DRAW);
    assertThat(breach.outcome()).isEqualTo(BreachOutcome.BLOCK);
    assertThat(breach.limitValue()).isEqualTo(5L);
    assertThat(breach.currentValue()).isEqualTo(5L); // existing count
    assertThat(breach.deltaValue()).isEqualTo(1L);   // lines in this ticket
  }

  @Test
  void two_lines_on_same_selection_accumulate_delta() {
    // 2 lines on "12" → delta=2; current=4, next=6 > 5 → breach
    var facts = factsWithCount(SCOPE, 4);
    var breaches = evaluate(5, facts, line("12"), line("12"));
    assertThat(breaches).hasSize(1);
    assertThat(breaches.get(0).deltaValue()).isEqualTo(2L);
  }

  @Test
  void different_selections_evaluated_independently() {
    // limit=3; "12" current=2 (+1=3, no breach), "34" current=3 (+1=4 > 3, breach)
    var facts =
        new LimitFactsSnapshot(
            Map.of(
                new LimitFactsSnapshot.Key(SCOPE, BetType.MATCH_1_2D, "12"),
                new LimitFactsSnapshot.Fact(0L, 2L),
                new LimitFactsSnapshot.Key(SCOPE, BetType.MATCH_1_2D, "34"),
                new LimitFactsSnapshot.Fact(0L, 3L)));

    var breaches = evaluate(3, facts, line("12"), line("34"));
    assertThat(breaches).hasSize(1);
    assertThat(breaches.get(0).currentValue()).isEqualTo(3L);
  }

  @Test
  void no_lines_produces_no_breaches() {
    var breaches = evaluate(5, emptyFacts());
    assertThat(breaches).isEmpty();
  }

  private List<LimitBreach> evaluate(long maxCount, LimitFactsSnapshot facts, LimitLineContext... lines) {
    var params = MAPPER.createObjectNode().put("maxCount", maxCount);
    var rule = new EffectiveLimitRule(
        RuleKey.MAX_SALES_COUNT_PER_SELECTION_PER_DRAW, BreachOutcome.BLOCK, SCOPE, null, params);
    var ctx = new LimitContext(
        TenantId.of(UUID.randomUUID()), null, null, null, null, Instant.now(), List.of(lines));
    return evaluator.evaluate(rule, facts, ctx);
  }

  private static LimitLineContext line(String selection) {
    return new LimitLineContext(BetType.MATCH_1_2D, selection, 500L);
  }

  private static LimitFactsSnapshot emptyFacts() {
    return new LimitFactsSnapshot(Map.of());
  }

  private static LimitFactsSnapshot factsWithCount(LimitScopeRef scope, long count) {
    return new LimitFactsSnapshot(
        Map.of(
            new LimitFactsSnapshot.Key(scope, BetType.MATCH_1_2D, "12"),
            new LimitFactsSnapshot.Fact(0L, count)));
  }
}
