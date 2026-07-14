package com.tchalanet.server.core.pricing.internal.application.command;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PricingRuleV0Policy")
class PricingRuleV0PolicyTest {

  @Test
  @DisplayName("MARYAJ_GRATIS requires FIXED_AMOUNT")
  void maryajGratisFixedAmount() {
    assertThatCode(
            () ->
                PricingRuleV0Policy.validateGameRuleType(
                    "HT_MARYAJ_GRATIS", PayoutRuleType.FIXED_AMOUNT))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("MARYAJ_GRATIS rejects STAKE_MULTIPLIER")
  void maryajGratisRejectsMultiplier() {
    assertThatThrownBy(
            () ->
                PricingRuleV0Policy.validateGameRuleType(
                    "HT_MARYAJ_GRATIS", PayoutRuleType.STAKE_MULTIPLIER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("regular game code requires STAKE_MULTIPLIER")
  void regularGameMultiplier() {
    assertThatCode(
            () ->
                PricingRuleV0Policy.validateGameRuleType(
                    "HT_BOLET", PayoutRuleType.STAKE_MULTIPLIER))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("regular game code rejects FIXED_AMOUNT")
  void regularGameRejectsFixed() {
    assertThatThrownBy(
            () -> PricingRuleV0Policy.validateGameRuleType("HT_BOLET", PayoutRuleType.FIXED_AMOUNT))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
