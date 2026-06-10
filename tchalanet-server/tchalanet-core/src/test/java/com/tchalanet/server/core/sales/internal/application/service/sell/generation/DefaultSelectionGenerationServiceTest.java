package com.tchalanet.server.core.sales.internal.application.service.sell.generation;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.model.selection.SelectionGenerationPurpose;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import com.tchalanet.server.core.selection.internal.application.DefaultSelectionApi;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultSelectionGenerationService")
class DefaultSelectionGenerationServiceTest {

    private static final short MARRIAGE_EXACT = 1;
    private static final int ITERATIONS = 500;

    private DefaultSelectionGenerationService service(long seed) {
        return new DefaultSelectionGenerationService(
            new RandomSelectionGenerator(new Random(seed)),
            new DefaultSelectionApi()
        );
    }

    @Test
    @DisplayName("maryaj gratuit: canonical distinct 2D pair NN-NN")
    void maryajGratuitPair() {
        var service = service(42);
        for (int i = 0; i < ITERATIONS; i++) {
            var selection = service.generate(
                GameCode.HT_MARYAJ_GRATUIT,
                BetType.MARRIAGE_2D2D,
                MARRIAGE_EXACT,
                SelectionGenerationStrategy.RANDOM,
                SelectionGenerationPurpose.PROMOTION_FREE_LINE
            );
            var key = selection.key().value();
            assertThat(key).matches("\\d{2}-\\d{2}");
            var parts = key.split("-");
            assertThat(parts[0]).isNotEqualTo(parts[1]);
        }
    }

    @Test
    @DisplayName("borlette: canonical 2 digits")
    void borletteTwoDigits() {
        var service = service(7);
        for (int i = 0; i < ITERATIONS; i++) {
            var selection = service.generate(
                GameCode.HT_BOLET,
                BetType.MATCH_1_2D,
                null,
                SelectionGenerationStrategy.RANDOM,
                SelectionGenerationPurpose.CASHIER_SUGGESTION
            );
            assertThat(selection.key().value()).matches("\\d{2}");
        }
    }

    @Test
    @DisplayName("loto3 straight: canonical 3 digits")
    void loto3ThreeDigits() {
        var selection = service(7).generate(
            GameCode.HT_LOTO3,
            BetType.LOTTO3_3D,
            (short) 1,
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.CASHIER_SUGGESTION
        );
        assertThat(selection.key().value()).matches("\\d{3}");
    }

    @Test
    @DisplayName("loto4 front/back pair: canonical star patterns")
    void loto4Patterns() {
        var front = service(7).generate(
            GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 3,
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.CASHIER_SUGGESTION
        );
        assertThat(front.key().value()).matches("\\d{2}\\*\\*");

        var back = service(8).generate(
            GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 4,
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.CASHIER_SUGGESTION
        );
        assertThat(back.key().value()).matches("\\*\\*\\d{2}");
    }

    @Test
    @DisplayName("loto5: canonical 5 digits")
    void loto5FiveDigits() {
        var selection = service(7).generate(
            GameCode.HT_LOTO5,
            BetType.LOTTO5_PATTERN,
            (short) 1,
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.CASHIER_SUGGESTION
        );
        assertThat(selection.key().value()).matches("\\d{5}");
    }

    @Test
    @DisplayName("LOW_EXPOSURE_RANDOM is rejected in V1")
    void lowExposureRejected() {
        assertThatThrownBy(() -> service(1).generate(
            GameCode.HT_MARYAJ_GRATUIT,
            BetType.MARRIAGE_2D2D,
            MARRIAGE_EXACT,
            SelectionGenerationStrategy.LOW_EXPOSURE_RANDOM,
            SelectionGenerationPurpose.PROMOTION_FREE_LINE
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("selection.generation.strategy_unsupported");
    }

    @Test
    @DisplayName("bet type not supported by game is rejected")
    void betTypeMismatchRejected() {
        assertThatThrownBy(() -> service(1).generate(
            GameCode.HT_BOLET,
            BetType.MARRIAGE_2D2D,
            MARRIAGE_EXACT,
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.PROMOTION_FREE_LINE
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("selection.generation.bet_type_not_supported_for_game");
    }

    @Test
    @DisplayName("maryaj without bet option is rejected")
    void maryajWithoutOptionRejected() {
        assertThatThrownBy(() -> service(1).generate(
            GameCode.HT_MARYAJ_GRATUIT,
            BetType.MARRIAGE_2D2D,
            null,
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.PROMOTION_FREE_LINE
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("betOption is required");
    }
}
