package com.tchalanet.server.core.sales.internal.application.command.handler.preparation;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffectType;
import com.tchalanet.server.core.sales.api.command.preparation.PrepareSaleCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.communication.SaleCommunicationOptions;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationView;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.internal.application.port.out.preparation.SalePreparationStorePort;
import com.tchalanet.server.core.sales.internal.application.sale.SaleEvaluationMode;
import com.tchalanet.server.core.sales.internal.application.service.preparation.SalePreparationInputCodec;
import com.tchalanet.server.core.sales.internal.application.service.preparation.SalePreparationViewAssembler;
import com.tchalanet.server.core.sales.internal.application.service.sell.SalePreparationOrchestrator;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationPromotionLine;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Prepare flow (DOMAIN_SALES.md §11): paidLines -> EvaluatePromotionQuery ->
 * FREE_GAME_LINE -> SelectionGenerationService (via the sell pipeline's
 * effect applier) -> persist SalePreparation -> SalePreparationView.
 */
@UseCase
@RequiredArgsConstructor
public class PrepareSaleCommandHandler
    implements CommandHandler<PrepareSaleCommand, SalePreparationView> {

    static final Duration TTL = Duration.ofMinutes(10);

    private final SalePreparationOrchestrator orchestrator;
    private final SalePreparationStorePort store;
    private final SalePreparationInputCodec codec;
    private final SalePreparationViewAssembler assembler;
    private final IdGenerator idGenerator;
    private final TchTimeProvider timeProvider;

    @Override
    @TchTx
    public SalePreparationView handle(PrepareSaleCommand cmd) {
        var ctx = TchContext.currentOrThrow();
        var sell = new SellTicketCommand(
            cmd.drawId(),
            cmd.drawChannelId(),
            cmd.currency(),
            cmd.lines(),
            cmd.communicationOptions() == null
                ? SaleCommunicationOptions.none()
                : cmd.communicationOptions(),
            List.of());

        var prepared = orchestrator.prepareSale(sell, ctx, SaleEvaluationMode.FINAL);

        var now = timeProvider.now();
        var preparation = store.create(new SalePreparation(
            idGenerator.newUuid(),
            SalePreparationStatus.DRAFT,
            prepared.sellerId() == null ? null : prepared.sellerId().value(),
            prepared.pos().salesSessionId() == null ? null : prepared.pos().salesSessionId().value(),
            prepared.pos().terminalId() == null ? null : prepared.pos().terminalId().value(),
            cmd.drawId().value(),
            codec.hash(sell),
            codec.toMap(sell),
            prepared.promotionDecision() == null
                ? null : prepared.promotionDecision().decisionId().value(),
            null,
            null,
            now.plus(TTL),
            null,
            promotionLines(prepared.ticketLines(), prepared.promotionDecision())));

        return assembler.toView(preparation, prepared);
    }

    private List<SalePreparationPromotionLine> promotionLines(
        List<TicketLine> finalLines, PromotionDecision decision) {
        var out = new ArrayList<SalePreparationPromotionLine>();
        for (var line : finalLines) {
            if (line.origin() != TicketLineOrigin.PROMOTION) {
                continue;
            }
            var effect = matchingEffect(decision, line);
            out.add(new SalePreparationPromotionLine(
                idGenerator.newUuid().toString(),
                line.gameCode().name(),
                line.betType().name(),
                line.betOption(),
                line.selection().key().value(),
                line.payoutBaseAmount().amount(),
                line.promotionDecisionId() == null ? null : line.promotionDecisionId().value(),
                effect == null || effect.ruleId() == null ? null : effect.ruleId().value(),
                effect != null && effect.regenerableBeforeConfirm(),
                effect == null
                    ? PromotionEffect.DEFAULT_MAX_REGENERATIONS
                    : effect.maxRegenerationsBeforeConfirm(),
                0));
        }
        return out;
    }

    private PromotionEffect matchingEffect(PromotionDecision decision, TicketLine line) {
        if (decision == null) {
            return null;
        }
        return decision.effects().stream()
            .filter(e -> e.type() == PromotionEffectType.FREE_GAME_LINE)
            .filter(e -> line.gameCode().name().equals(e.gameCode()))
            .findFirst()
            .orElse(null);
    }
}
