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

class BlockBetTypeEvaluatorTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final LimitScopeRef SCOPE = LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));

  private final BlockBetTypeEvaluator evaluator = new BlockBetTypeEvaluator();

  @Test
  void supports_the_block_bet_type_rule() {
    assertThat(evaluator.supports()).isEqualTo(RuleKey.BLOCK_BET_TYPE);
  }

  @Test
  void lines_of_an_unblocked_bet_type_do_not_breach() {
    var breaches = evaluate(BetType.LOTTO3_3D, line(BetType.MATCH_1_2D), line(BetType.MATCH_2_2D));
    assertThat(breaches).isEmpty();
  }

  @Test
  void a_line_of_the_blocked_bet_type_breaches() {
    var breaches = evaluate(BetType.LOTTO3_3D, line(BetType.MATCH_1_2D), line(BetType.LOTTO3_3D));

    assertThat(breaches).hasSize(1);
    assertThat(breaches.get(0).ruleKey()).isEqualTo(RuleKey.BLOCK_BET_TYPE);
    assertThat(breaches.get(0).outcome()).isEqualTo(BreachOutcome.BLOCK);
  }

  @Test
  void each_blocked_line_produces_its_own_breach() {
    var breaches = evaluate(BetType.LOTTO3_3D, line(BetType.LOTTO3_3D), line(BetType.LOTTO3_3D));
    assertThat(breaches).hasSize(2);
  }

  private List<LimitBreach> evaluate(BetType blocked, LimitLineContext... lines) {
    var params = MAPPER.createObjectNode().put("betType", blocked.name());
    var rule =
        new EffectiveLimitRule(RuleKey.BLOCK_BET_TYPE, BreachOutcome.BLOCK, SCOPE, null, params);
    return evaluator.evaluate(rule, new LimitFactsSnapshot(Map.of()), context(lines));
  }

  private static LimitLineContext line(BetType betType) {
    return new LimitLineContext(betType, "05", 100L);
  }

  private static LimitContext context(LimitLineContext... lines) {
    return new LimitContext(
        TenantId.of(UUID.randomUUID()), null, null, null, null, Instant.now(), List.of(lines));
  }
}
