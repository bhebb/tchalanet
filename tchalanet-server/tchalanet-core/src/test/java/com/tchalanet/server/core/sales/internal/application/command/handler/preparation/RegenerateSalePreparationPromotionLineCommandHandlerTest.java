package com.tchalanet.server.core.sales.internal.application.command.handler.preparation;

import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.command.preparation.RegenerateSalePreparationPromotionLineCommand;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.application.service.preparation.SalePreparationViewAssembler;
import com.tchalanet.server.core.sales.internal.application.service.sell.generation.DefaultSelectionGenerationService;
import com.tchalanet.server.core.sales.internal.application.service.sell.generation.RandomSelectionGenerator;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationPromotionLine;
import com.tchalanet.server.core.selection.internal.application.DefaultSelectionApi;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RegenerateSalePreparationPromotionLineCommandHandler")
class RegenerateSalePreparationPromotionLineCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-06-10T12:00:00Z");
    private static final UUID PREP_ID = UUID.randomUUID();

    private final InMemorySalePreparationStore store = new InMemorySalePreparationStore();

    private RegenerateSalePreparationPromotionLineCommandHandler handler() {
        return new RegenerateSalePreparationPromotionLineCommandHandler(
            store,
            new DefaultSelectionGenerationService(
                new RandomSelectionGenerator(), new DefaultSelectionApi()),
            new SalePreparationViewAssembler(),
            new TimeProvider(Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private SalePreparation draft(
        SalePreparationStatus status, Instant expiresAt, boolean regenerable, int count, int max) {
        return new SalePreparation(
            PREP_ID, status, null, null, null, UUID.randomUUID(),
            "hash", Map.of(), null, null, null, expiresAt, null,
            List.of(new SalePreparationPromotionLine(
                "ref-1", "HT_MARYAJ_GRATUIT", "MARRIAGE_2D2D", (short) 1, "34-78",
                new BigDecimal("50"), null, null, regenerable, max, count)));
    }

    @Test
    @DisplayName("regenerates: replaces the selection and increments the counter")
    void regenerates() {
        store.create(draft(SalePreparationStatus.DRAFT, NOW.plusSeconds(60), true, 0, 3));

        var view = handler().handle(
            new RegenerateSalePreparationPromotionLineCommand(PREP_ID, "ref-1"));

        var line = view.promotionLines().get(0);
        assertThat(line.selection()).matches("\\d{2}-\\d{2}");
        assertThat(line.regenerationsRemaining()).isEqualTo(2);
        assertThat(store.byId.get(PREP_ID).promotionLines().get(0).regenerationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("max regenerations reached is rejected")
    void maxReached() {
        store.create(draft(SalePreparationStatus.DRAFT, NOW.plusSeconds(60), true, 3, 3));

        assertThatThrownBy(() -> handler().handle(
            new RegenerateSalePreparationPromotionLineCommand(PREP_ID, "ref-1")))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("max_regenerations_reached");
    }

    @Test
    @DisplayName("non-regenerable line is rejected")
    void notRegenerable() {
        store.create(draft(SalePreparationStatus.DRAFT, NOW.plusSeconds(60), false, 0, 3));

        assertThatThrownBy(() -> handler().handle(
            new RegenerateSalePreparationPromotionLineCommand(PREP_ID, "ref-1")))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("line_not_regenerable");
    }

    @Test
    @DisplayName("after confirm, regeneration is rejected")
    void afterConfirmRejected() {
        store.create(draft(SalePreparationStatus.CONFIRMED, NOW.plusSeconds(60), true, 0, 3));

        assertThatThrownBy(() -> handler().handle(
            new RegenerateSalePreparationPromotionLineCommand(PREP_ID, "ref-1")))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("already_confirmed");
    }

    @Test
    @DisplayName("expired preparation is rejected and marked EXPIRED")
    void expiredRejected() {
        store.create(draft(SalePreparationStatus.DRAFT, NOW.minusSeconds(1), true, 0, 3));

        assertThatThrownBy(() -> handler().handle(
            new RegenerateSalePreparationPromotionLineCommand(PREP_ID, "ref-1")))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("expired");
        assertThat(store.byId.get(PREP_ID).status()).isEqualTo(SalePreparationStatus.EXPIRED);
    }
}
