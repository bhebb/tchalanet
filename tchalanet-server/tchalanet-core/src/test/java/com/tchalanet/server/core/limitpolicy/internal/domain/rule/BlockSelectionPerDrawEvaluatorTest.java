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

class BlockSelectionPerDrawEvaluatorTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final LimitScopeRef SCOPE = LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));
  private static final LimitFactsSnapshot EMPTY_FACTS = new LimitFactsSnapshot(Map.of());

  private final BlockSelectionPerDrawEvaluator evaluator = new BlockSelectionPerDrawEvaluator();

  @Test
  void supports_block_selection_per_draw() {
    assertThat(evaluator.supports()).isEqualTo(RuleKey.BLOCK_SELECTION_PER_DRAW);
  }

  @Test
  void blocked_selection_without_bet_type_filter_breaches() {
    var breaches = evaluate(null, "34", line(BetType.MATCH_1_2D, "34"));
    assertThat(breaches).hasSize(1);
    assertThat(breaches.get(0).ruleKey()).isEqualTo(RuleKey.BLOCK_SELECTION_PER_DRAW);
  }

  @Test
  void non_blocked_selection_without_filter_does_not_breach() {
    var breaches = evaluate(null, "34", line(BetType.MATCH_1_2D, "07"));
    assertThat(breaches).isEmpty();
  }

  @Test
  void bet_type_filter_matches_and_selection_blocked_breaches() {
    var breaches = evaluate("MATCH_1_2D", "34", line(BetType.MATCH_1_2D, "34"));
    assertThat(breaches).hasSize(1);
  }

  @Test
  void bet_type_filter_mismatch_skips_line_even_if_selection_matches() {
    var breaches = evaluate("MATCH_2_2D", "34", line(BetType.MATCH_1_2D, "34"));
    assertThat(breaches).isEmpty();
  }

  @Test
  void only_offending_lines_breach_when_multiple_lines() {
    var ctx =
        context(
            line(BetType.MATCH_1_2D, "34"), // blocked
            line(BetType.MATCH_1_2D, "07"), // not blocked
            line(BetType.MATCH_1_2D, "34")); // blocked again
    var breaches = evaluate(null, "34", ctx);
    assertThat(breaches).hasSize(2);
  }

  @Test
  void empty_selections_list_never_breaches() {
    var breaches = evaluate(null, /* selections= */ new String[0], line(BetType.MATCH_1_2D, "34"));
    assertThat(breaches).isEmpty();
  }

  @Test
  void no_lines_produces_no_breaches() {
    var breaches = evaluate(null, "34");
    assertThat(breaches).isEmpty();
  }

  private List<LimitBreach> evaluate(
      String betTypeName, String blockedSelection, LimitLineContext... lines) {
    return evaluate(betTypeName, new String[] {blockedSelection}, lines);
  }

  private List<LimitBreach> evaluate(
      String betTypeName, String[] selections, LimitLineContext... lines) {
    var params = MAPPER.createObjectNode();
    if (betTypeName != null) {
      params.put("betType", betTypeName);
    }
    var arr = params.putArray("selections");
    for (var s : selections) {
      arr.add(s);
    }
    var rule =
        new EffectiveLimitRule(
            RuleKey.BLOCK_SELECTION_PER_DRAW, BreachOutcome.BLOCK, SCOPE, null, params);
    return evaluator.evaluate(rule, EMPTY_FACTS, context(lines));
  }

  private List<LimitBreach> evaluate(
      String betTypeName, String blockedSelection, LimitContext ctx) {
    var params = MAPPER.createObjectNode();
    if (betTypeName != null) {
      params.put("betType", betTypeName);
    }
    params.putArray("selections").add(blockedSelection);
    var rule =
        new EffectiveLimitRule(
            RuleKey.BLOCK_SELECTION_PER_DRAW, BreachOutcome.BLOCK, SCOPE, null, params);
    return evaluator.evaluate(rule, EMPTY_FACTS, ctx);
  }

  private static LimitLineContext line(BetType betType, String selection) {
    return new LimitLineContext(betType, selection, 1000L);
  }

  private static LimitContext context(LimitLineContext... lines) {
    return new LimitContext(
        TenantId.of(UUID.randomUUID()), null, null, null, null, Instant.now(), List.of(lines));
  }
}
