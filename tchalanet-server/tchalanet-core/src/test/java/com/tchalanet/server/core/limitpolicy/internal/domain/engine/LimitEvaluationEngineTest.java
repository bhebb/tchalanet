package com.tchalanet.server.core.limitpolicy.internal.domain.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimits;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;
import com.tchalanet.server.core.limitpolicy.internal.domain.rule.LimitRuleEvaluator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LimitEvaluationEngineTest {

  private static final LimitScopeRef SCOPE = LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));

  // --- construction -------------------------------------------------------

  @Test
  void rejects_null_or_empty_evaluator_list() {
    assertThatThrownBy(() -> new LimitEvaluationEngine(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LimitEvaluationEngine(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_a_null_evaluator_in_the_list() {
    var withNull = new ArrayList<LimitRuleEvaluator>();
    withNull.add(null);
    assertThatThrownBy(() -> new LimitEvaluationEngine(withNull))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_an_evaluator_returning_a_null_rule_key() {
    assertThatThrownBy(() -> new LimitEvaluationEngine(List.of(new FakeEvaluator(null, List.of()))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejects_two_evaluators_for_the_same_rule_key() {
    var a = new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, List.of());
    var b = new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, List.of());
    assertThatThrownBy(() -> new LimitEvaluationEngine(List.of(a, b)))
        .isInstanceOf(IllegalStateException.class);
  }

  // --- evaluate argument guards -------------------------------------------

  @Test
  void evaluate_requires_all_arguments() {
    var engine = engineWith(new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, List.of()));
    assertThatThrownBy(() -> engine.evaluate(null, facts(), context()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> engine.evaluate(EffectiveLimits.empty(), null, context()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> engine.evaluate(EffectiveLimits.empty(), facts(), null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- evaluation ---------------------------------------------------------

  @Test
  void no_rules_configured_allows_the_sale() {
    var engine =
        engineWith(new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, breach(BreachOutcome.BLOCK)));

    var result = engine.evaluate(EffectiveLimits.empty(), facts(), context());

    assertThat(result.outcome()).isEqualTo(BreachOutcome.ALLOW);
    assertThat(result.breaches()).isEmpty();
    assertThat(result.allowed()).isTrue();
  }

  @Test
  void a_rule_without_a_registered_evaluator_fails_fast() {
    // engine only knows MAX_LINES_PER_TICKET, but the limits carry MAX_STAKE_PER_LINE
    var engine = engineWith(new FakeEvaluator(RuleKey.MAX_LINES_PER_TICKET, List.of()));
    var limits = limitsWith(RuleKey.MAX_STAKE_PER_LINE, BreachOutcome.BLOCK);

    assertThatThrownBy(() -> engine.evaluate(limits, facts(), context()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void a_single_blocking_breach_blocks_the_sale() {
    var engine =
        engineWith(new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, breach(BreachOutcome.BLOCK)));
    var limits = limitsWith(RuleKey.MAX_STAKE_PER_LINE, BreachOutcome.BLOCK);

    var result = engine.evaluate(limits, facts(), context());

    assertThat(result.outcome()).isEqualTo(BreachOutcome.BLOCK);
    assertThat(result.blocked()).isTrue();
    assertThat(result.breaches()).hasSize(1);
  }

  @Test
  void a_warn_only_breach_still_allows_the_sale() {
    var engine =
        engineWith(new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, breach(BreachOutcome.WARN)));
    var limits = limitsWith(RuleKey.MAX_STAKE_PER_LINE, BreachOutcome.WARN);

    var result = engine.evaluate(limits, facts(), context());

    assertThat(result.outcome()).isEqualTo(BreachOutcome.WARN);
    assertThat(result.allowed()).isTrue();
  }

  @Test
  void the_most_severe_outcome_wins_across_rules() {
    var warn = new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, breach(BreachOutcome.WARN));
    var block = new FakeEvaluator(RuleKey.MAX_STAKE_PER_TICKET, breach(BreachOutcome.BLOCK));
    var engine = engineWith(warn, block);

    var limits =
        new EffectiveLimits(
            Map.of(
                RuleKey.MAX_STAKE_PER_LINE, rule(RuleKey.MAX_STAKE_PER_LINE, BreachOutcome.WARN),
                RuleKey.MAX_STAKE_PER_TICKET,
                    rule(RuleKey.MAX_STAKE_PER_TICKET, BreachOutcome.BLOCK)));

    var result = engine.evaluate(limits, facts(), context());

    assertThat(result.outcome()).isEqualTo(BreachOutcome.BLOCK);
    assertThat(result.breaches()).hasSize(2);
  }

  @Test
  void require_approval_is_treated_as_blocked() {
    var engine =
        engineWith(
            new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, breach(BreachOutcome.REQUIRE_APPROVAL)));
    var limits = limitsWith(RuleKey.MAX_STAKE_PER_LINE, BreachOutcome.REQUIRE_APPROVAL);

    var result = engine.evaluate(limits, facts(), context());

    assertThat(result.outcome()).isEqualTo(BreachOutcome.REQUIRE_APPROVAL);
    assertThat(result.blocked()).isTrue();
    assertThat(result.allowed()).isFalse();
  }

  @Test
  void an_evaluator_returning_null_is_treated_as_no_breach() {
    var engine = engineWith(new FakeEvaluator(RuleKey.MAX_STAKE_PER_LINE, null));
    var limits = limitsWith(RuleKey.MAX_STAKE_PER_LINE, BreachOutcome.BLOCK);

    var result = engine.evaluate(limits, facts(), context());

    assertThat(result.outcome()).isEqualTo(BreachOutcome.ALLOW);
    assertThat(result.breaches()).isEmpty();
  }

  // --- helpers ------------------------------------------------------------

  private static LimitEvaluationEngine engineWith(LimitRuleEvaluator... evaluators) {
    return new LimitEvaluationEngine(List.of(evaluators));
  }

  private static EffectiveLimits limitsWith(RuleKey key, BreachOutcome onBreach) {
    return new EffectiveLimits(Map.of(key, rule(key, onBreach)));
  }

  private static EffectiveLimitRule rule(RuleKey key, BreachOutcome onBreach) {
    return new EffectiveLimitRule(key, onBreach, SCOPE, null, null);
  }

  private static List<LimitBreach> breach(BreachOutcome outcome) {
    return List.of(
        new LimitBreach(
            RuleKey.MAX_STAKE_PER_LINE, outcome, SCOPE, "code", "messageKey", 100L, null, 200L));
  }

  private static LimitFactsSnapshot facts() {
    return new LimitFactsSnapshot(Map.of());
  }

  private static LimitContext context() {
    return new LimitContext(
        TenantId.of(UUID.randomUUID()), null, null, null, null, Instant.now(), List.of());
  }

  /** Test double: reports a fixed rule key and returns preconfigured breaches. */
  private static final class FakeEvaluator implements LimitRuleEvaluator {

    private final RuleKey key;
    private final List<LimitBreach> toReturn;

    FakeEvaluator(RuleKey key, List<LimitBreach> toReturn) {
      this.key = key;
      this.toReturn = toReturn;
    }

    @Override
    public RuleKey supports() {
      return key;
    }

    @Override
    public List<LimitBreach> evaluate(
        EffectiveLimitRule rule, LimitFactsSnapshot facts, LimitContext ctx) {
      return toReturn;
    }
  }
}
