package com.tchalanet.server.core.pricing.internal.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PricingRuleV0Policy")
class PricingRuleV0PolicyTest {

  @Test
  @DisplayName("MARYAJ_GRATIS expects FIXED_AMOUNT")
  void maryajGratisExpectedRuleType() {
    assertThat(PricingRuleV0Policy.expectedRuleType("HT_MARYAJ_GRATIS"))
        .isEqualTo(PayoutRuleType.FIXED_AMOUNT);
  }

  @Test
  @DisplayName("regular game code expects STAKE_MULTIPLIER")
  void regularGameExpectedRuleType() {
    assertThat(PricingRuleV0Policy.expectedRuleType("HT_BOLET"))
        .isEqualTo(PayoutRuleType.STAKE_MULTIPLIER);
  }
}
