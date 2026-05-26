package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionEffect;
import com.tchalanet.server.core.sales.internal.application.port.out.AppliedPromotionSnapshotWriterPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.AppliedPromotionSnapshotJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.AppliedPromotionSnapshotRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AppliedPromotionSnapshotJpaAdapter implements AppliedPromotionSnapshotWriterPort {
    private final AppliedPromotionSnapshotRepository repository;

    @Override
    public void createIfAbsent(TicketId ticketId, PromotionDecision decision, Instant appliedAt) {
        if (decision == null || decision.decisionId() == null) {
            return;
        }
        if (repository.findByTicketIdAndPromotionDecisionId(ticketId.value(), decision.decisionId().value()).isPresent()) {
            return;
        }

        var entity = new AppliedPromotionSnapshotJpaEntity();
        entity.setTicketId(ticketId.value());
        entity.setPromotionDecisionId(decision.decisionId().value());
        entity.setDecisionStatus(decision.status().name());
        entity.setAppliedAt(appliedAt);
        entity.setSnapshotJson(toSnapshotJson(decision));
        repository.save(entity);
    }

    private Map<String, Object> toSnapshotJson(PromotionDecision decision) {
        var json = new LinkedHashMap<String, Object>();
        json.put("decisionId", decision.decisionId().value().toString());
        json.put("status", decision.status().name());
        json.put("phase", decision.phase().name());
        json.put("evaluatedAt", decision.evaluatedAt().toString());
        json.put("contextHash", decision.contextHash());
        json.put("engineVersion", decision.engineVersion());
        json.put("notices", decision.notices());
        json.put("effects", decision.effects().stream().map(this::toEffectJson).toList());
        return json;
    }

    private Map<String, Object> toEffectJson(PromotionEffect effect) {
        var json = new LinkedHashMap<String, Object>();
        json.put("ruleId", effect.ruleId() == null ? null : effect.ruleId().value().toString());
        json.put("type", effect.type() == null ? null : effect.type().name());
        json.put("gameCode", effect.gameCode());
        json.put("quantity", effect.quantity());
        json.put("amount", effect.amount());
        json.put("currency", effect.currency());
        json.put("appliesTo", effect.appliesTo());
        json.put("reason", effect.reason());
        json.put("choiceMode", effect.choiceMode() == null ? null : effect.choiceMode().name());
        return json;
    }
}
