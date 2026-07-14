package com.tchalanet.server.core.sales.internal.domain.service.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.sales.api.model.coverage.SettlementPayoutMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SettlementVariantResolver")
class SettlementVariantResolverTest {

  @Nested
  @DisplayName("Bòlèt 2D lots")
  class Bolet {

    @Test
    @DisplayName("MATCH_1_2D resolves to MATCH_1_2D")
    void match1() {
      assertThat(SettlementVariantResolver.resolve(BetType.MATCH_1_2D, null, "36"))
          .isEqualTo(PricingVariantCode.MATCH_1_2D);
    }

    @Test
    @DisplayName("MATCH_2_2D resolves to MATCH_2_2D")
    void match2() {
      assertThat(SettlementVariantResolver.resolve(BetType.MATCH_2_2D, null, "17"))
          .isEqualTo(PricingVariantCode.MATCH_2_2D);
    }

    @Test
    @DisplayName("MATCH_3_2D resolves to MATCH_3_2D")
    void match3() {
      assertThat(SettlementVariantResolver.resolve(BetType.MATCH_3_2D, null, "76"))
          .isEqualTo(PricingVariantCode.MATCH_3_2D);
    }
  }

  @Nested
  @DisplayName("Maryaj")
  class Maryaj {

    @Test
    @DisplayName("option 1 resolves to MARRIAGE_EXACT_ORDER")
    void exactOrder() {
      assertThat(SettlementVariantResolver.resolve(BetType.MARRIAGE_2D2D, (short) 1, "36-17"))
          .isEqualTo(PricingVariantCode.MARRIAGE_EXACT_ORDER);
    }

    @Test
    @DisplayName("option 2 resolves to MARRIAGE_REVERSE_ALLOWED")
    void reverseAllowed() {
      assertThat(SettlementVariantResolver.resolve(BetType.MARRIAGE_2D2D, (short) 2, "36-17"))
          .isEqualTo(PricingVariantCode.MARRIAGE_REVERSE_ALLOWED);
    }

    @Test
    @DisplayName("unsupported option code is rejected")
    void unsupportedOption() {
      assertThatThrownBy(
              () -> SettlementVariantResolver.resolve(BetType.MARRIAGE_2D2D, (short) 99, "36-17"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Loto 3")
  class Lotto3 {

    @Test
    @DisplayName("option 1 resolves to LOTTO3_STRAIGHT")
    void straight() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO3_3D, (short) 1, "736"))
          .isEqualTo(PricingVariantCode.LOTTO3_STRAIGHT);
    }

    @Test
    @DisplayName("box with 2 identical digits resolves to 3-way")
    void box3Way() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO3_3D, (short) 2, "112"))
          .isEqualTo(PricingVariantCode.LOTTO3_BOX_3_WAY);
    }

    @Test
    @DisplayName("box with 3 different digits resolves to 6-way")
    void box6Way() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO3_3D, (short) 2, "736"))
          .isEqualTo(PricingVariantCode.LOTTO3_BOX_6_WAY);
    }

    @Test
    @DisplayName("box with all identical digits is rejected")
    void boxAllIdentical() {
      assertThatThrownBy(
              () -> SettlementVariantResolver.resolve(BetType.LOTTO3_3D, (short) 2, "111"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("box with wrong digit count is rejected")
    void boxWrongLength() {
      assertThatThrownBy(
              () -> SettlementVariantResolver.resolve(BetType.LOTTO3_3D, (short) 2, "12"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("exact plus box resolves to straight and box coverages")
    void exactPlusBoxCoverage() {
      var resolution =
          SettlementVariantResolver.resolveCoverage(BetType.LOTTO3_3D, (short) 3, "736");

      assertThat(resolution.settlementPayoutMode())
          .isEqualTo(SettlementPayoutMode.RANGE_ALTERNATIVE);
      assertThat(resolution.variants())
          .extracting(CoverageVariant::pricingVariantCode)
          .containsExactly(PricingVariantCode.LOTTO3_STRAIGHT, PricingVariantCode.LOTTO3_BOX_6_WAY);
      assertThatThrownBy(
              () -> SettlementVariantResolver.resolve(BetType.LOTTO3_3D, (short) 3, "736"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Loto 4")
  class Lotto4 {

    @Test
    @DisplayName("option 1 resolves to LOTTO4_STRAIGHT")
    void straight() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 1, "1234"))
          .isEqualTo(PricingVariantCode.LOTTO4_STRAIGHT);
    }

    @Test
    @DisplayName("box 3 identical digits resolves to 4-way")
    void box4Way() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 2, "1112"))
          .isEqualTo(PricingVariantCode.LOTTO4_BOX_4_WAY);
    }

    @Test
    @DisplayName("box 2 pairs resolves to 6-way")
    void box6Way() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 2, "1122"))
          .isEqualTo(PricingVariantCode.LOTTO4_BOX_6_WAY);
    }

    @Test
    @DisplayName("box 1 pair + 2 different resolves to 12-way")
    void box12Way() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 2, "1123"))
          .isEqualTo(PricingVariantCode.LOTTO4_BOX_12_WAY);
    }

    @Test
    @DisplayName("box 4 different digits resolves to 24-way")
    void box24Way() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 2, "1234"))
          .isEqualTo(PricingVariantCode.LOTTO4_BOX_24_WAY);
    }

    @Test
    @DisplayName("box all identical digits is rejected")
    void boxAllIdentical() {
      assertThatThrownBy(
              () -> SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 2, "1111"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("option 3 resolves to LOTTO4_FRONT_PAIR")
    void frontPair() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 3, "12"))
          .isEqualTo(PricingVariantCode.LOTTO4_FRONT_PAIR);
    }

    @Test
    @DisplayName("option 4 resolves to LOTTO4_BACK_PAIR")
    void backPair() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 4, "34"))
          .isEqualTo(PricingVariantCode.LOTTO4_BACK_PAIR);
    }

    @Test
    @DisplayName("unsupported option code is rejected")
    void unsupportedOption() {
      assertThatThrownBy(
              () -> SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 99, "1234"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("exact plus box resolves to straight and box coverages")
    void exactPlusBoxCoverage() {
      var resolution =
          SettlementVariantResolver.resolveCoverage(BetType.LOTTO4_PATTERN, (short) 5, "1123");

      assertThat(resolution.settlementPayoutMode())
          .isEqualTo(SettlementPayoutMode.RANGE_ALTERNATIVE);
      assertThat(resolution.variants())
          .extracting(CoverageVariant::pricingVariantCode)
          .containsExactly(
              PricingVariantCode.LOTTO4_STRAIGHT, PricingVariantCode.LOTTO4_BOX_12_WAY);
      assertThatThrownBy(
              () -> SettlementVariantResolver.resolve(BetType.LOTTO4_PATTERN, (short) 5, "1123"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Loto 5")
  class Lotto5 {

    @Test
    @DisplayName("option 1 resolves to LOTTO5_LOT1_LOT2")
    void lot1Lot2() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO5_PATTERN, (short) 1, "36175"))
          .isEqualTo(PricingVariantCode.LOTTO5_LOT1_LOT2);
    }

    @Test
    @DisplayName("option 2 resolves to LOTTO5_LOT1_LOT3")
    void lot1Lot3() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO5_PATTERN, (short) 2, "36176"))
          .isEqualTo(PricingVariantCode.LOTTO5_LOT1_LOT3);
    }

    @Test
    @DisplayName("option 3 resolves to LOTTO5_MIXED_1_2_3")
    void mixed() {
      assertThat(SettlementVariantResolver.resolve(BetType.LOTTO5_PATTERN, (short) 3, "17576"))
          .isEqualTo(PricingVariantCode.LOTTO5_MIXED_1_2_3);
    }
  }

  @Test
  @DisplayName("null betType is rejected")
  void nullBetType() {
    assertThatThrownBy(() -> SettlementVariantResolver.resolve(null, (short) 1, "1234"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
