package com.tchalanet.server.core.reconciliation.internal.application;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationAnomaly;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationAnomalyJpaEntity;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationAnomalyJpaRepository;
import java.time.Instant;

@UseCase
public class ReconciliationAnomalyWriter {

    private final ReconciliationAnomalyJpaRepository repository;

    public ReconciliationAnomalyWriter(ReconciliationAnomalyJpaRepository repository) {
        this.repository = repository;
    }

    public ReconciliationAnomalyJpaEntity upsert(ReconciliationAnomaly anomaly, Instant now) {
        return repository.save(repository
            .findByTenantIdAndFingerprint(anomaly.tenantId().value(), anomaly.fingerprint())
            .map(existing -> updateSeen(existing, anomaly, now))
            .orElseGet(() -> create(anomaly, now)));
    }

    private ReconciliationAnomalyJpaEntity updateSeen(
        ReconciliationAnomalyJpaEntity existing,
        ReconciliationAnomaly anomaly,
        Instant now
    ) {
        existing.setRunId(anomaly.runId().value());
        existing.setLastSeenAt(now);
        existing.setStatus(anomaly.status());
        existing.setSeverity(anomaly.severity());
        existing.setMessage(anomaly.message());
        existing.setDetailsJson(anomaly.detailsJson());
        return existing;
    }

    private ReconciliationAnomalyJpaEntity create(ReconciliationAnomaly anomaly, Instant now) {
        var entity = new ReconciliationAnomalyJpaEntity();
        entity.setId(anomaly.id().value());
        entity.setTenantId(anomaly.tenantId().value());
        entity.setRunId(anomaly.runId().value());
        entity.setBusinessDate(anomaly.businessDate());
        entity.setSeverity(anomaly.severity());
        entity.setAnomalyType(anomaly.anomalyType());
        entity.setStatus(anomaly.status());
        entity.setFingerprint(anomaly.fingerprint());
        entity.setDrawId(anomaly.drawId() == null ? null : anomaly.drawId().value());
        entity.setDrawChannelId(anomaly.drawChannelId() == null ? null : anomaly.drawChannelId().value());
        entity.setDrawResultId(anomaly.drawResultId() == null ? null : anomaly.drawResultId().value());
        entity.setTicketId(anomaly.ticketId() == null ? null : anomaly.ticketId().value());
        entity.setTicketCode(anomaly.ticketCode());
        entity.setPublicCode(anomaly.publicCode());
        entity.setDisplayCode(anomaly.displayCode());
        entity.setPayoutClaimId(anomaly.payoutClaimId());
        entity.setPayoutPaymentId(anomaly.payoutPaymentId() == null ? null : anomaly.payoutPaymentId().value());
        entity.setExpectedStatus(anomaly.expectedStatus());
        entity.setActualStatus(anomaly.actualStatus());
        entity.setExpectedAmount(anomaly.expectedAmount());
        entity.setActualAmount(anomaly.actualAmount());
        entity.setCurrency(anomaly.currency());
        entity.setMessage(anomaly.message());
        entity.setDetailsJson(anomaly.detailsJson());
        entity.setFirstSeenAt(now);
        entity.setLastSeenAt(now);
        entity.setResolvedAt(anomaly.resolvedAt());
        return entity;
    }
}
