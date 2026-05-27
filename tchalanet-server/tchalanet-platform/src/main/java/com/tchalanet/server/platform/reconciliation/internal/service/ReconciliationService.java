package com.tchalanet.server.platform.reconciliation.internal.service;

import com.tchalanet.server.platform.reconciliation.internal.persistence.ReconciliationRunJpaEntity;
import com.tchalanet.server.platform.reconciliation.internal.persistence.ReconciliationRunJpaRepository;
import com.tchalanet.server.platform.reconciliation.internal.service.event.ReconciliationRunCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Thin orchestration service for reconciliation runs.
 * - Creates a run record
 * - (placeholder) executes checks
 * - Marks run completed and publishes event
 *
 * Heavy reconciliation logic should live in dedicated check handlers and use QueryBus to fetch read models.
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationRunJpaRepository runRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UUID startRun(UUID tenantId, String scope, LocalDate businessDate) {
        ReconciliationRunJpaEntity run = new ReconciliationRunJpaEntity();
        run.setTenantId(tenantId);
        run.setScope(scope);
        run.setBusinessDate(businessDate);
        run.setStartedAt(Instant.now());
        run.setStatus("RUNNING");
        run = runRepository.save(run);

        // Placeholder: run checks asynchronously / via dedicated check handlers

        // mark completed for now
        run.setCompletedAt(Instant.now());
        run.setStatus("COMPLETED");
        run = runRepository.save(run);

        // publish completion event
        eventPublisher.publishEvent(new ReconciliationRunCompletedEvent(this, run.getId(), tenantId, businessDate));
        return run.getId();
    }
}

