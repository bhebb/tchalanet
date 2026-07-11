package com.tchalanet.server.catalog.game.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BetOptionTest {

    @Test
    void noOptionBetTypesAcceptMissingOptionOnly() {
        assertThat(BetOption.allowedFor(BetType.MATCH_1_2D)).isEmpty();
        assertThat(BetOption.requiresOption(BetType.MATCH_1_2D)).isFalse();
        assertThat(BetOption.from(BetType.MATCH_1_2D, null)).isNull();

        assertThatThrownBy(() -> BetOption.from(BetType.MATCH_1_2D, (short) 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not support options");
    }

    @Test
    void maryajOptionsAreExactOrReverseAllowed() {
        assertThat(BetOption.allowedFor(BetType.MARRIAGE_2D2D))
            .containsExactly(
                BetOption.MARRIAGE_EXACT_ORDER,
                BetOption.MARRIAGE_REVERSE_ALLOWED);

        assertThat(BetOption.requiresOption(BetType.MARRIAGE_2D2D)).isTrue();
        assertThat(BetOption.from(BetType.MARRIAGE_2D2D, (short) 1))
            .isEqualTo(BetOption.MARRIAGE_EXACT_ORDER);
        assertThat(BetOption.from(BetType.MARRIAGE_2D2D, (short) 2))
            .isEqualTo(BetOption.MARRIAGE_REVERSE_ALLOWED);
    }

    @Test
    void lottoOptionsAreScopedByBetType() {
        assertThat(BetOption.allowedFor(BetType.LOTTO3_3D))
            .containsExactly(
                BetOption.LOTTO3_STRAIGHT,
                BetOption.LOTTO3_BOX,
                BetOption.LOTTO3_EXACT_PLUS_BOX);

        assertThat(BetOption.allowedFor(BetType.LOTTO4_PATTERN))
            .containsExactly(
                BetOption.LOTTO4_STRAIGHT,
                BetOption.LOTTO4_BOX,
                BetOption.LOTTO4_FRONT_PAIR,
                BetOption.LOTTO4_BACK_PAIR,
                BetOption.LOTTO4_EXACT_PLUS_BOX);

        assertThat(BetOption.allowedFor(BetType.LOTTO5_PATTERN))
            .containsExactly(
                BetOption.LOTTO5_LOT1_LOT2,
                BetOption.LOTTO5_LOT1_LOT3,
                BetOption.LOTTO5_MIXED_1_2_3);

        assertThat(BetOption.from(BetType.LOTTO4_PATTERN, (short) 5))
            .isEqualTo(BetOption.LOTTO4_EXACT_PLUS_BOX);
    }

    @Test
    void optionBetTypesRejectMissingOrUnsupportedCodes() {
        assertThatThrownBy(() -> BetOption.from(BetType.LOTTO4_PATTERN, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("betOption is required");

        assertThatThrownBy(() -> BetOption.from(BetType.LOTTO4_PATTERN, (short) 6))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported betOption");

        assertThatThrownBy(() -> BetOption.from(null, (short) 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("betType is required");
    }

    @Test
    void betTypeDelegatesOptionHelpersToBetOptionCatalog() {
        assertThat(BetType.LOTTO4_PATTERN.allowedOptions()).isEqualTo(BetOption.allowedFor(BetType.LOTTO4_PATTERN));
        assertThat(BetType.LOTTO4_PATTERN.supportsOption((short) 5)).isTrue();
        assertThat(BetType.LOTTO4_PATTERN.supportsOption((short) 6)).isFalse();
        assertThat(BetType.LOTTO4_PATTERN.betOptionMax()).isEqualTo((short) 5);
    }

    @Test
    void exactPlusBoxOptionsAreNotEnabledByDefault() {
        assertThat(BetOption.LOTTO3_STRAIGHT.enabledByDefault()).isTrue();
        assertThat(BetOption.LOTTO3_EXACT_PLUS_BOX.enabledByDefault()).isFalse();
        assertThat(BetOption.LOTTO4_EXACT_PLUS_BOX.enabledByDefault()).isFalse();
    }
}
