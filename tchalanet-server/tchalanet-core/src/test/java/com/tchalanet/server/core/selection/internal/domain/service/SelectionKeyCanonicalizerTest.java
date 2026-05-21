package com.tchalanet.server.core.selection.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.game.api.model.BetType;
import org.junit.jupiter.api.Test;

class SelectionKeyCanonicalizerTest {

    @Test
    void canonicalizesSimple2dAndLotto3Selections() {
        assertThat(SelectionKeyCanonicalizer.canonicalize(BetType.MATCH_1_2D, null, "5").value())
            .isEqualTo("05");
        assertThat(SelectionKeyCanonicalizer.canonicalize(BetType.LOTTO3_3D, (short) 1, "23").value())
            .isEqualTo("023");
    }

    @Test
    void maryajPreservesSellerOrder() {
        assertThat(SelectionKeyCanonicalizer.canonicalize(BetType.MARRIAGE_2D2D, (short) 1, "45/12").value())
            .isEqualTo("45-12");
    }

    @Test
    void loto4CanonicalizationIsOptionAware() {
        assertThat(SelectionKeyCanonicalizer.canonicalize(BetType.LOTTO4_PATTERN, (short) 1, "1245").value())
            .isEqualTo("1245");
        assertThat(SelectionKeyCanonicalizer.canonicalize(BetType.LOTTO4_PATTERN, (short) 2, "1245").value())
            .isEqualTo("1245");
        assertThat(SelectionKeyCanonicalizer.canonicalize(BetType.LOTTO4_PATTERN, (short) 3, "12").value())
            .isEqualTo("12**");
        assertThat(SelectionKeyCanonicalizer.canonicalize(BetType.LOTTO4_PATTERN, (short) 4, "45").value())
            .isEqualTo("**45");
    }

    @Test
    void cashierWildcardInputIsRejected() {
        assertThatThrownBy(() -> SelectionKeyCanonicalizer.canonicalize(
            BetType.LOTTO4_PATTERN,
            (short) 1,
            "12**"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("selection must be numeric");
    }

    @Test
    void loto5RequiresFiveDigits() {
        assertThat(SelectionKeyCanonicalizer.canonicalize(BetType.LOTTO5_PATTERN, (short) 3, "12345").value())
            .isEqualTo("12345");

        assertThatThrownBy(() -> SelectionKeyCanonicalizer.canonicalize(BetType.LOTTO5_PATTERN, (short) 3, "1234"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("selection must be 5 digits");
    }
}
