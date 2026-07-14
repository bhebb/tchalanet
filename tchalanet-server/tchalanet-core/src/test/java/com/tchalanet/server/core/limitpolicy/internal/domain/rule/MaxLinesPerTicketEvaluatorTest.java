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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MaxLinesPerTicketEvaluatorTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final LimitScopeRef SCOPE = LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));

  private final MaxLinesPerTicketEvaluator evaluator = new MaxLinesPerTicketEvaluator();

  @Test
  void supports_the_max_lines_per_ticket_rule() {
    assertThat(evaluator.supports()).isEqualTo(RuleKey.MAX_LINES_PER_TICKET);
  }

  @Test
  void a_line_count_at_or_below_the_limit_does_not_breach() {
    assertThat(evaluate(3, 3)).isEmpty();
    assertThat(evaluate(3, 2)).isEmpty();
  }

  @Test
  void a_line_count_above_the_limit_breaches_with_the_count_reported() {
    var breaches = evaluate(3, 4);

    assertThat(breaches).hasSize(1);
    var breach = breaches.get(0);
    assertThat(breach.ruleKey()).isEqualTo(RuleKey.MAX_LINES_PER_TICKET);
    assertThat(breach.limitValue()).isEqualTo(3L);
    assertThat(breach.deltaValue()).isEqualTo(4L);
  }

  private List<LimitBreach> evaluate(long maxCount, int lineCount) {
    var params = MAPPER.createObjectNode().put("maxCount", maxCount);
    var rule =
        new EffectiveLimitRule(
            RuleKey.MAX_LINES_PER_TICKET, BreachOutcome.BLOCK, SCOPE, null, params);
    return evaluator.evaluate(rule, new LimitFactsSnapshot(Map.of()), context(lineCount));
  }

  private static LimitContext context(int lineCount) {
    var lines = new ArrayList<LimitLineContext>();
    for (int i = 0; i < lineCount; i++) {
      lines.add(new LimitLineContext(BetType.MATCH_1_2D, "0" + i, 100L));
    }
    return new LimitContext(
        TenantId.of(UUID.randomUUID()), null, null, null, null, Instant.now(), lines);
  }
}
