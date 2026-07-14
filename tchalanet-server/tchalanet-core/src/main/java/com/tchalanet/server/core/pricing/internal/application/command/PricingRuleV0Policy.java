package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;

final class PricingRuleV0Policy {

  private static final String MARYAJ_GRATIS_GAME_CODE = "HT_MARYAJ_GRATIS";

  private PricingRuleV0Policy() {}

  static void validateGameRuleType(String gameCode, PayoutRuleType ruleType) {
    var expected = expectedRuleType(gameCode);
    if (ruleType != expected) {
      throw new IllegalArgumentException(
          "gameCode " + gameCode + " requires payoutRuleType " + expected);
    }
  }

  private static PayoutRuleType expectedRuleType(String gameCode) {
    return MARYAJ_GRATIS_GAME_CODE.equals(gameCode)
        ? PayoutRuleType.FIXED_AMOUNT
        : PayoutRuleType.STAKE_MULTIPLIER;
  }
}
