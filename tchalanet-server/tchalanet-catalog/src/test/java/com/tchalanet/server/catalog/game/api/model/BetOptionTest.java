package com.tchalanet.server.catalog.game.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BetOptionTest {

    @Test
    void optionRequirementsAreScopedByBetType() {
        assertThat(BetOption.requiresOption(BetType.MATCH_1_2D)).isFalse();
        assertThat(BetOption.requiresOption(BetType.MARRIAGE_2D2D)).isTrue();
        assertThat(BetOption.requiresOption(BetType.LOTTO3_3D)).isTrue();
        assertThat(BetOption.requiresOption(BetType.LOTTO4_PATTERN)).isTrue();
        assertThat(BetOption.requiresOption(BetType.LOTTO5_PATTERN)).isTrue();
    }

    @Test
    void fromReturnsNullWhenBetTypeDoesNotSupportOptionsAndCodeIsNull() {
        assertThat(BetOption.from(BetType.MATCH_1_2D, null)).isNull();
    }

    @Test
    void fromRejectsMissingOrUnsupportedOptions() {
        assertThatThrownBy(() -> BetOption.from(BetType.LOTTO4_PATTERN, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("betOption is required");

        assertThatThrownBy(() -> BetOption.from(BetType.LOTTO4_PATTERN, (short) 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported betOption");

        assertThatThrownBy(() -> BetOption.from(BetType.MATCH_1_2D, (short) 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not support options");
    }

    @Test
    void betTypeDelegatesOptionSupportToBetOption() {
        assertThat(BetType.LOTTO4_PATTERN.allowedOptions())
            .containsExactly(
                BetOption.LOTTO4_STRAIGHT,
                BetOption.LOTTO4_BOX,
                BetOption.LOTTO4_FRONT_PAIR,
                BetOption.LOTTO4_BACK_PAIR);
        assertThat(BetType.LOTTO4_PATTERN.supportsOption((short) 4)).isTrue();
        assertThat(BetType.LOTTO4_PATTERN.betOptionMax()).isEqualTo((short) 4);
    }
}
