package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import com.tchalanet.server.core.sales.internal.application.port.out.preparation.SalePreparationStorePort;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparation;
import com.tchalanet.server.core.sales.internal.domain.model.preparation.SalePreparationPromotionLine;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.preparation.SalePreparationJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.preparation.SalePreparationPromotionLineJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.SalePreparationPromotionLineRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.SalePreparationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SalePreparationJpaAdapter implements SalePreparationStorePort {

    private final SalePreparationRepository preparationRepository;
    private final SalePreparationPromotionLineRepository lineRepository;

    @Override
    public SalePreparation create(SalePreparation preparation) {
        var entity = new SalePreparationJpaEntity();
        entity.setId(preparation.id());
        entity.setSellerId(preparation.sellerId());
        entity.setSessionId(preparation.sessionId());
        entity.setTerminalId(preparation.terminalId());
        entity.setDrawId(preparation.drawId());
        entity.setStatus(preparation.status());
        entity.setInputHash(preparation.inputHash());
        entity.setPaidLinesJson(preparation.input());
        entity.setPromotionDecisionId(preparation.promotionDecisionId());
        entity.setExpiresAt(preparation.expiresAt());
        var saved = preparationRepository.save(entity);

        for (var line : preparation.promotionLines()) {
            var lineEntity = new SalePreparationPromotionLineJpaEntity();
            lineEntity.setPreparationId(saved.getId());
            lineEntity.setLineRef(line.lineRef());
            lineEntity.setGameCode(line.gameCode());
            lineEntity.setBetType(line.betType());
            lineEntity.setBetOption(line.betOption());
            lineEntity.setSelection(line.selection());
            lineEntity.setPayoutBaseAmount(line.payoutBaseAmount());
            lineEntity.setPromotionDecisionId(line.promotionDecisionId());
            lineEntity.setPromotionRuleId(line.promotionRuleId());
            lineEntity.setRegenerable(line.regenerable());
            lineEntity.setMaxRegenerations(line.maxRegenerations());
            lineEntity.setRegenerationCount(line.regenerationCount());
            lineRepository.save(lineEntity);
        }
        return findById(saved.getId()).orElseThrow();
    }

    @Override
    public Optional<SalePreparation> findById(UUID preparationId) {
        return preparationRepository.findById(preparationId).map(this::toDomain);
    }

    @Override
    public void updateStatus(UUID preparationId, SalePreparationStatus status) {
        var entity = getRequired(preparationId);
        entity.setStatus(status);
        preparationRepository.save(entity);
    }

    @Override
    public void updateLineSelection(
        UUID preparationId, String lineRef, String selection, int regenerationCount) {
        var line = lineRepository.findByPreparationIdAndLineRef(preparationId, lineRef)
            .orElseThrow(() -> ProblemRest.notFound("sales.preparation.promotion_line_not_found"));
        line.setSelection(selection);
        line.setRegenerationCount(regenerationCount);
        lineRepository.save(line);
    }

    @Override
    public void confirm(UUID preparationId, UUID ticketId, String idempotencyKey, Instant confirmedAt) {
        var entity = getRequired(preparationId);
        entity.setStatus(SalePreparationStatus.CONFIRMED);
        entity.setTicketId(ticketId);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setConfirmedAt(confirmedAt);
        preparationRepository.save(entity);
    }

    private SalePreparationJpaEntity getRequired(UUID preparationId) {
        return preparationRepository.findById(preparationId)
            .orElseThrow(() -> ProblemRest.notFound("sales.preparation.not_found"));
    }

    private SalePreparation toDomain(SalePreparationJpaEntity entity) {
        var lines = lineRepository.findByPreparationIdOrderByLineRefAsc(entity.getId()).stream()
            .map(l -> new SalePreparationPromotionLine(
                l.getLineRef(),
                l.getGameCode(),
                l.getBetType(),
                l.getBetOption(),
                l.getSelection(),
                l.getPayoutBaseAmount(),
                l.getPromotionDecisionId(),
                l.getPromotionRuleId(),
                l.isRegenerable(),
                l.getMaxRegenerations(),
                l.getRegenerationCount()))
            .toList();
        return new SalePreparation(
            entity.getId(),
            entity.getStatus(),
            entity.getSellerId(),
            entity.getSessionId(),
            entity.getTerminalId(),
            entity.getDrawId(),
            entity.getInputHash(),
            entity.getPaidLinesJson(),
            entity.getPromotionDecisionId(),
            entity.getIdempotencyKey(),
            entity.getTicketId(),
            entity.getExpiresAt(),
            entity.getConfirmedAt(),
            lines);
    }
}
