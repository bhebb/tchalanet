package com.tchalanet.server.core.sales.internal.application.command.handler.preparation;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.preparation.RegenerateSalePreparationPromotionLineCommand;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationView;
import com.tchalanet.server.core.sales.api.model.selection.SelectionGenerationPurpose;
import com.tchalanet.server.core.sales.internal.application.port.out.preparation.SalePreparationStorePort;
import com.tchalanet.server.core.sales.internal.application.service.preparation.SalePreparationViewAssembler;
import com.tchalanet.server.core.sales.internal.application.service.sell.generation.SelectionGenerationService;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Regenerate guards: DRAFT non expirée, ligne PROMOTION régénérable,
 * compteur < max. Remplace la sélection (pas d'historique de lignes) ;
 * chaque régénération est auditée (actor/session/terminal).
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class RegenerateSalePreparationPromotionLineCommandHandler
    implements CommandHandler<RegenerateSalePreparationPromotionLineCommand, SalePreparationView> {

    private final SalePreparationStorePort store;
    private final SelectionGenerationService selectionGenerationService;
    private final SalePreparationViewAssembler assembler;
    private final TchTimeProvider timeProvider;

    @Override
    @TchTx
    public SalePreparationView handle(RegenerateSalePreparationPromotionLineCommand cmd) {
        var preparation = store.findById(cmd.preparationId())
            .orElseThrow(() -> ProblemRest.notFound("sales.preparation.not_found"));

        requireDraftNotExpired(preparation);

        var line = preparation.line(cmd.lineRef())
            .orElseThrow(() -> ProblemRest.notFound("sales.preparation.promotion_line_not_found"));

        if (!line.regenerable()) {
            throw ProblemRest.conflict("sales.preparation.line_not_regenerable");
        }
        if (line.regenerationCount() >= line.maxRegenerations()) {
            throw ProblemRest.conflict("sales.preparation.max_regenerations_reached");
        }

        var selection = selectionGenerationService.generate(
            GameCode.valueOf(line.gameCode()),
            BetType.valueOf(line.betType()),
            line.betOption(),
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.PROMOTION_FREE_LINE);

        store.updateLineSelection(
            preparation.id(), line.lineRef(), selection.key().value(), line.regenerationCount() + 1);

        var ctx = TchContext.currentOrNull();
        log.info("sales: promotion line regenerated preparation={} lineRef={} count={} actor={} session={} terminal={}",
            preparation.id(), line.lineRef(), line.regenerationCount() + 1,
            ctx == null ? null : ctx.userId(),
            preparation.sessionId(), preparation.terminalId());

        var reloaded = store.findById(preparation.id()).orElseThrow();
        return assembler.toView(reloaded);
    }

    private void requireDraftNotExpired(SalePreparation preparation) {
        if (preparation.isExpired(timeProvider.now())) {
            store.updateStatus(preparation.id(), SalePreparationStatus.EXPIRED);
            throw ProblemRest.conflict("sales.preparation.expired");
        }
        if (preparation.status() != SalePreparationStatus.DRAFT) {
            throw switch (preparation.status()) {
                case CONFIRMED -> ProblemRest.conflict("sales.preparation.already_confirmed");
                case EXPIRED -> ProblemRest.conflict("sales.preparation.expired");
                case CANCELLED -> ProblemRest.conflict("sales.preparation.cancelled");
                case DRAFT -> new IllegalStateException("unreachable");
            };
        }
    }
}
