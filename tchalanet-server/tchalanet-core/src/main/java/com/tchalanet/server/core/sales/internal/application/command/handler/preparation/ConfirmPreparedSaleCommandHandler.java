package com.tchalanet.server.core.sales.internal.application.command.handler.preparation;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.preparation.ConfirmPreparedSaleCommand;
import com.tchalanet.server.core.sales.api.command.sell.PromotionChoiceInput;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.model.preparation.ConfirmPreparedSaleResult;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.internal.application.port.out.preparation.SalePreparationStorePort;
import com.tchalanet.server.core.sales.internal.application.service.preparation.SalePreparationInputCodec;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationPromotionLine;
import com.tchalanet.server.core.sales.internal.domain.service.preparation.SalePreparationStateMachine;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationTransition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * Confirm: payload = preparationId + idempotencyKey only. Rejoue le pipeline
 * sell standard (validation money/limits/session conservée) avec les
 * sélections promotionnelles épinglées depuis la préparation — les numéros du
 * ticket sont exactement ceux du dernier preview. Double confirm même
 * idempotencyKey -> même ticket. Aucune régénération possible ici.
 */
@UseCase
@RequiredArgsConstructor
public class ConfirmPreparedSaleCommandHandler
    implements CommandHandler<ConfirmPreparedSaleCommand, ConfirmPreparedSaleResult> {

    private final SalePreparationStorePort store;
    private final SalePreparationInputCodec codec;
    private final SalePreparationStateMachine stateMachine;
    private final CommandBus commandBus;
    private final TchTimeProvider timeProvider;

    @Override
    @TchTx
    public ConfirmPreparedSaleResult handle(ConfirmPreparedSaleCommand cmd) {
        var preparation = store.findById(cmd.preparationId())
            .orElseThrow(() -> ProblemRest.notFound("sales.preparation.not_found"));

        if (preparation.status() == SalePreparationStatus.CONFIRMED) {
            if (Objects.equals(preparation.idempotencyKey(), cmd.idempotencyKey())
                && preparation.ticketId() != null) {
                return new ConfirmPreparedSaleResult(
                    preparation.id(), preparation.ticketId(), true, null);
            }
            throw ProblemRest.conflict("sales.preparation.already_confirmed");
        }
        if (preparation.isExpired(timeProvider.now())) {
            store.updateStatus(preparation.id(), SalePreparationStatus.EXPIRED);
            throw ProblemRest.conflict("sales.preparation.expired");
        }
        if (preparation.status() != SalePreparationStatus.DRAFT) {
            throw ProblemRest.conflict("sales.preparation.not_confirmable");
        }

        var sell = codec.fromMap(preparation.input(), pinnedChoices(preparation));
        var result = commandBus.execute(sell);

        if (result.outcome() == SellTicketOutcome.REJECTED) {
            return new ConfirmPreparedSaleResult(preparation.id(), null, false, result);
        }

        stateMachine.apply(preparation.status(), SalePreparationTransition.CONFIRM);
        store.confirm(
            preparation.id(),
            result.ticketId().value(),
            cmd.idempotencyKey(),
            timeProvider.now());

        return new ConfirmPreparedSaleResult(
            preparation.id(), result.ticketId().value(), false, result);
    }

    /**
     * Pins each stored generated selection on the rebuilt command. Index is
     * per gameCode group order (quantity = 1 in V1). decisionId is left null:
     * the sell pipeline issues a fresh decision id at confirm and
     * PromotionSelectionResolver matches choices by gameCode + index.
     */
    private List<PromotionChoiceInput> pinnedChoices(SalePreparation preparation) {
        var out = new ArrayList<PromotionChoiceInput>();
        var indexByGame = new HashMap<String, Integer>();
        for (SalePreparationPromotionLine line : preparation.promotionLines()) {
            int index = indexByGame.merge(line.gameCode(), 0, (a, b) -> a + 1);
            out.add(new PromotionChoiceInput(
                null,
                line.gameCode(),
                index,
                line.selection(),
                TicketLineSelectionSource.PROMOTION_GENERATED));
        }
        return out;
    }
}
