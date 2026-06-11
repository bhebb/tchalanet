package com.tchalanet.server.core.sales.internal.application.command.handler.preparation;

import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.application.port.out.preparation.SalePreparationStorePort;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationPromotionLine;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemorySalePreparationStore implements SalePreparationStorePort {

    final Map<UUID, SalePreparation> byId = new HashMap<>();

    @Override
    public SalePreparation create(SalePreparation preparation) {
        byId.put(preparation.id(), preparation);
        return preparation;
    }

    @Override
    public Optional<SalePreparation> findById(UUID preparationId) {
        return Optional.ofNullable(byId.get(preparationId));
    }

    @Override
    public void updateStatus(UUID preparationId, SalePreparationStatus status) {
        var p = byId.get(preparationId);
        byId.put(preparationId, new SalePreparation(
            p.id(), status, p.sellerId(), p.sessionId(), p.terminalId(), p.drawId(),
            p.inputHash(), p.input(), p.promotionDecisionId(), p.idempotencyKey(),
            p.ticketId(), p.expiresAt(), p.confirmedAt(), p.promotionLines()));
    }

    @Override
    public void updateLineSelection(
        UUID preparationId, String lineRef, String selection, int regenerationCount) {
        var p = byId.get(preparationId);
        var lines = p.promotionLines().stream()
            .map(l -> l.lineRef().equals(lineRef)
                ? new SalePreparationPromotionLine(
                    l.lineRef(), l.gameCode(), l.betType(), l.betOption(), selection,
                    l.payoutBaseAmount(), l.promotionDecisionId(), l.promotionRuleId(),
                    l.regenerable(), l.maxRegenerations(), regenerationCount)
                : l)
            .toList();
        byId.put(preparationId, new SalePreparation(
            p.id(), p.status(), p.sellerId(), p.sessionId(), p.terminalId(), p.drawId(),
            p.inputHash(), p.input(), p.promotionDecisionId(), p.idempotencyKey(),
            p.ticketId(), p.expiresAt(), p.confirmedAt(), lines));
    }

    @Override
    public void confirm(UUID preparationId, UUID ticketId, String idempotencyKey, Instant confirmedAt) {
        var p = byId.get(preparationId);
        byId.put(preparationId, new SalePreparation(
            p.id(), SalePreparationStatus.CONFIRMED, p.sellerId(), p.sessionId(), p.terminalId(),
            p.drawId(), p.inputHash(), p.input(), p.promotionDecisionId(), idempotencyKey,
            ticketId, p.expiresAt(), confirmedAt, p.promotionLines()));
    }
}
