package com.tchalanet.server.core.sales.internal.application.service.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.sales.api.settlement.ComputedSettlementVariantView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SettlementExplanationService")
class SettlementExplanationServiceTest {

    private final SettlementExplanationService service = new SettlementExplanationService();

    @Test
    @DisplayName("Loto 4 box 4-different-digits: commercial Permuté, admin 24-way")
    void lotto4Box24Way() {
        ComputedSettlementVariantView view =
            service.explain(BetType.LOTTO4_PATTERN, (short) 2, "1234");

        assertThat(view.variant()).isEqualTo("LOTTO4_BOX_24_WAY");
        assertThat(view.commercialLabel()).isEqualTo("Désordre / Box");
        assertThat(view.adminLabel()).isEqualTo("Permuté · 24-way");
    }

    @Test
    @DisplayName("Loto 4 box one-pair: admin 12-way")
    void lotto4Box12Way() {
        assertThat(service.explain(BetType.LOTTO4_PATTERN, (short) 2, "1123").adminLabel())
            .isEqualTo("Permuté · 12-way");
    }

    @Test
    @DisplayName("Loto 3 straight: commercial and admin both Exact")
    void lotto3Straight() {
        var view = service.explain(BetType.LOTTO3_3D, (short) 1, "736");

        assertThat(view.variant()).isEqualTo("LOTTO3_STRAIGHT");
        assertThat(view.commercialLabel()).isEqualTo("Exact");
        assertThat(view.adminLabel()).isEqualTo("Exact");
    }

    @Test
    @DisplayName("Bòlèt option-less: commercial equals admin label")
    void boletLotLabel() {
        var view = service.explain(BetType.MATCH_1_2D, null, "36");

        assertThat(view.variant()).isEqualTo("MATCH_1_2D");
        assertThat(view.commercialLabel()).isEqualTo("1er lot");
        assertThat(view.adminLabel()).isEqualTo("1er lot");
    }

    @Test
    @DisplayName("Maryaj reverse: admin label exposes the maryaj variant")
    void maryajReverse() {
        assertThat(service.explain(BetType.MARRIAGE_2D2D, (short) 2, "36-17").adminLabel())
            .isEqualTo("Maryaj · revers/double");
    }

    @Test
    @DisplayName("unsupported option propagates as IllegalArgumentException")
    void unsupportedRejected() {
        assertThatThrownBy(() -> service.explain(BetType.LOTTO4_PATTERN, (short) 99, "1234"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
