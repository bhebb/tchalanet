package com.tchalanet.server.core.limitpolicy.internal.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LimitRuleParamsTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final LimitScopeRef SCOPE = LimitScopeRef.tenant(TenantId.of(UUID.randomUUID()));

  // --- requiredLong ---

  @Test
  void requiredLong_returns_value_when_field_is_numeric() {
    var rule = rule(MAPPER.createObjectNode().put("valueCents", 5000L));
    assertThat(LimitRuleParams.requiredLong(rule, "valueCents")).isEqualTo(5000L);
  }

  @Test
  void requiredLong_throws_when_field_is_text() {
    var rule = rule(MAPPER.createObjectNode().put("valueCents", "oops"));
    assertThatThrownBy(() -> LimitRuleParams.requiredLong(rule, "valueCents"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must be numeric");
  }

  @Test
  void requiredLong_throws_when_field_is_missing() {
    var rule = rule(MAPPER.createObjectNode());
    assertThatThrownBy(() -> LimitRuleParams.requiredLong(rule, "valueCents"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing limit param");
  }

  @Test
  void requiredLong_throws_when_params_are_null() {
    var rule = rule(null);
    assertThatThrownBy(() -> LimitRuleParams.requiredLong(rule, "valueCents"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing params");
  }

  // --- requiredText ---

  @Test
  void requiredText_returns_value_when_field_is_textual() {
    var rule = rule(MAPPER.createObjectNode().put("betType", "MATCH_1_2D"));
    assertThat(LimitRuleParams.requiredText(rule, "betType")).isEqualTo("MATCH_1_2D");
  }

  @Test
  void requiredText_throws_when_field_is_numeric() {
    var rule = rule(MAPPER.createObjectNode().put("betType", 42));
    assertThatThrownBy(() -> LimitRuleParams.requiredText(rule, "betType"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must be text");
  }

  @Test
  void requiredText_throws_when_field_is_missing() {
    var rule = rule(MAPPER.createObjectNode());
    assertThatThrownBy(() -> LimitRuleParams.requiredText(rule, "betType"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing limit param");
  }

  // --- hasText ---

  @Test
  void hasText_returns_true_when_field_has_non_blank_text() {
    var rule = rule(MAPPER.createObjectNode().put("betType", "MATCH_1_2D"));
    assertThat(LimitRuleParams.hasText(rule, "betType")).isTrue();
  }

  @Test
  void hasText_returns_false_when_params_are_null() {
    var rule = rule(null);
    assertThat(LimitRuleParams.hasText(rule, "betType")).isFalse();
  }

  @Test
  void hasText_returns_false_when_field_is_missing() {
    var rule = rule(MAPPER.createObjectNode());
    assertThat(LimitRuleParams.hasText(rule, "betType")).isFalse();
  }

  @Test
  void hasText_returns_false_when_field_is_blank() {
    var rule = rule(MAPPER.createObjectNode().put("betType", "   "));
    assertThat(LimitRuleParams.hasText(rule, "betType")).isFalse();
  }

  @Test
  void hasText_returns_false_when_field_is_numeric_not_text() {
    var rule = rule(MAPPER.createObjectNode().put("betType", 42));
    assertThat(LimitRuleParams.hasText(rule, "betType")).isFalse();
  }

  // --- stringArrayContains ---

  @Test
  void stringArrayContains_returns_true_when_value_is_in_array() {
    var params = MAPPER.createObjectNode();
    params.putArray("selections").add("12").add("34");
    var rule = rule(params);
    assertThat(LimitRuleParams.stringArrayContains(rule, "selections", "34")).isTrue();
  }

  @Test
  void stringArrayContains_returns_false_when_value_is_not_in_array() {
    var params = MAPPER.createObjectNode();
    params.putArray("selections").add("12");
    var rule = rule(params);
    assertThat(LimitRuleParams.stringArrayContains(rule, "selections", "99")).isFalse();
  }

  @Test
  void stringArrayContains_returns_false_when_params_are_null() {
    var rule = rule(null);
    assertThat(LimitRuleParams.stringArrayContains(rule, "selections", "12")).isFalse();
  }

  @Test
  void stringArrayContains_returns_false_when_field_is_not_array() {
    var rule = rule(MAPPER.createObjectNode().put("selections", "12"));
    assertThat(LimitRuleParams.stringArrayContains(rule, "selections", "12")).isFalse();
  }

  @Test
  void stringArrayContains_returns_false_when_value_is_null() {
    var params = MAPPER.createObjectNode();
    params.putArray("selections").add("12");
    var rule = rule(params);
    assertThat(LimitRuleParams.stringArrayContains(rule, "selections", null)).isFalse();
  }

  @Test
  void stringArrayContains_returns_false_when_array_is_empty() {
    var params = MAPPER.createObjectNode();
    params.putArray("selections");
    var rule = rule(params);
    assertThat(LimitRuleParams.stringArrayContains(rule, "selections", "12")).isFalse();
  }

  private static EffectiveLimitRule rule(tools.jackson.databind.JsonNode params) {
    return new EffectiveLimitRule(RuleKey.BLOCK_SELECTION_PER_DRAW, BreachOutcome.BLOCK, SCOPE, null, params);
  }
}
