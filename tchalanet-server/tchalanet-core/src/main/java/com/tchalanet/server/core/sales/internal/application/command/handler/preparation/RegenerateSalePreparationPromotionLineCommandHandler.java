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
import com.tchalanet.server.core.sales.api.error.SalesErrorCodes;
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
 * Regenerate guards: DRAFT non expirée, ligne PROMOTION régénérable, compteur < max. Remplace la
 * sélection (pas d'historique de lignes).
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
    var preparation =
        store
            .findById(cmd.preparationId())
            .orElseThrow(() -> ProblemRest.of(SalesErrorCodes.PREPARATION_NOT_FOUND));

    requireDraftNotExpired(preparation);

    var line =
        preparation
            .line(cmd.lineRef())
            .orElseThrow(
                () -> ProblemRest.of(SalesErrorCodes.PREPARATION_PROMOTION_LINE_NOT_FOUND));

    if (!line.regenerable()) {
      throw ProblemRest.of(SalesErrorCodes.PREPARATION_LINE_NOT_REGENERABLE);
    }
    if (line.regenerationCount() >= line.maxRegenerations()) {
      throw ProblemRest.of(SalesErrorCodes.PREPARATION_MAX_REGENERATIONS_REACHED);
    }

    var selection =
        selectionGenerationService.generate(
            GameCode.valueOf(line.gameCode()),
            BetType.valueOf(line.betType()),
            line.betOption(),
            SelectionGenerationStrategy.RANDOM,
            SelectionGenerationPurpose.PROMOTION_FREE_LINE);

    store.updateLineSelection(
        preparation.id(), line.lineRef(), selection.key().value(), line.regenerationCount() + 1);

    var ctx = TchContext.currentOrNull();
    log.info(
        "sales: promotion line regenerated preparation={} lineRef={} count={} actor={}",
        preparation.id(),
        line.lineRef(),
        line.regenerationCount() + 1,
        ctx == null ? null : ctx.userId());

    var reloaded = store.findById(preparation.id()).orElseThrow();
    return assembler.toView(reloaded);
  }

  private void requireDraftNotExpired(SalePreparation preparation) {
    if (preparation.isExpired(timeProvider.now())) {
      store.updateStatus(preparation.id(), SalePreparationStatus.EXPIRED);
      throw ProblemRest.of(SalesErrorCodes.PREPARATION_EXPIRED);
    }
    if (preparation.status() != SalePreparationStatus.DRAFT) {
      throw switch (preparation.status()) {
        case CONFIRMED -> ProblemRest.of(SalesErrorCodes.PREPARATION_ALREADY_CONFIRMED);
        case EXPIRED -> ProblemRest.of(SalesErrorCodes.PREPARATION_EXPIRED);
        case CANCELLED -> ProblemRest.of(SalesErrorCodes.PREPARATION_CANCELLED);
        case DRAFT -> new IllegalStateException("unreachable");
      };
    }
  }
}
