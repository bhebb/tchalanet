package com.tchalanet.server.core.reconciliation.internal.application;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationRunStatus;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationRunType;
import com.tchalanet.server.core.reconciliation.internal.infra.batch.daily.DailyReconciliationProcessingResult;
import com.tchalanet.server.core.reconciliation.internal.infra.batch.daily.DailyReconciliationProcessor;
import com.tchalanet.server.core.reconciliation.internal.infra.batch.daily.DailyReconciliationReader;
import com.tchalanet.server.core.reconciliation.internal.infra.batch.daily.DailyReconciliationWriter;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationRunJpaEntity;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationRunJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

@UseCase
public class ReconciliationDailyRunService {

    private final IdGenerator idGenerator;
    private final ReconciliationRunJpaRepository runRepository;
    private final DailyReconciliationReader reader;
    private final DailyReconciliationProcessor processor;
    private final DailyReconciliationWriter writer;
    private final ReconciliationNotificationService notificationService;

    public ReconciliationDailyRunService(
        IdGenerator idGenerator,
        ReconciliationRunJpaRepository runRepository,
        DailyReconciliationReader reader,
        DailyReconciliationProcessor processor,
        DailyReconciliationWriter writer,
        ReconciliationNotificationService notificationService
    ) {
        this.idGenerator = idGenerator;
        this.runRepository = runRepository;
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
        this.notificationService = notificationService;
    }

    @TchTx
    public ReconciliationDailyRunResult forceDailyRun(
        TenantId tenantId,
        LocalDate businessDate,
        String reason
    ) {
        validateForceRun(tenantId, businessDate, reason);
        return runDaily(tenantId, businessDate, reason.trim(), ReconciliationRunType.FORCED, true);
    }

    public ReconciliationDailyRunResult scheduledDailyRun(
        TenantId tenantId,
        LocalDate businessDate
    ) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (businessDate == null) {
            throw new IllegalArgumentException("businessDate is required");
        }
        return runDaily(tenantId, businessDate, "scheduled daily reconciliation", ReconciliationRunType.SCHEDULED, false);
    }

    private ReconciliationDailyRunResult runDaily(
        TenantId tenantId,
        LocalDate businessDate,
        String reason,
        ReconciliationRunType runType,
        boolean forced
    ) {
        var startedAt = Instant.now();
        var runId = ReconciliationRunId.of(idGenerator.newUuid());
        var run = createRun(runId, tenantId, businessDate, reason, runType, forced, startedAt);
        var items = reader.read(runId, tenantId, businessDate);
        var processed = new ArrayList<DailyReconciliationProcessingResult>();
        for (var item : items) {
            processed.add(processor.process(item, startedAt));
        }

        var counters = writer.write(processed, Instant.now());
        var completedAt = Instant.now();
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setCompletedAt(completedAt);
        run.setCheckedDrawCount(counters.checkedDrawCount());
        run.setCheckedTicketCount(counters.checkedTicketCount());
        run.setAnomalyCount(counters.anomalyCount());
        run.setCriticalCount(counters.criticalCount());
        run.setHighCount(counters.highCount());
        run.setMediumCount(counters.mediumCount());
        run.setLowCount(counters.lowCount());
        runRepository.save(run);
        AfterCommit.run(() -> notificationService.enqueueAnomalyEmail(
            new ReconciliationNotificationService.ReconciliationRunNotification(
                runId,
                tenantId,
                businessDate,
                counters.checkedDrawCount(),
                counters.checkedTicketCount(),
                counters.anomalyCount(),
                counters.criticalCount(),
                counters.highCount(),
                counters.mediumCount()
            )));

        return new ReconciliationDailyRunResult(
            runId,
            tenantId,
            businessDate,
            run.getStatus().name(),
            (int) counters.anomalyCount(),
            run.getStartedAt(),
            completedAt,
            reason,
            counters.checkedDrawCount(),
            counters.checkedTicketCount(),
            counters.criticalCount(),
            counters.highCount(),
            counters.mediumCount(),
            counters.lowCount()
        );
    }

    private void validateForceRun(TenantId tenantId, LocalDate businessDate, String reason) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (businessDate == null) {
            throw new IllegalArgumentException("businessDate is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reconciliation.force_reason_required");
        }
    }

    private ReconciliationRunJpaEntity createRun(
        ReconciliationRunId runId,
        TenantId tenantId,
        LocalDate businessDate,
        String reason,
        ReconciliationRunType runType,
        boolean forced,
        Instant now
    ) {
        var run = new ReconciliationRunJpaEntity();
        run.setId(runId.value());
        run.setTenantId(tenantId.value());
        run.setBusinessDate(businessDate);
        run.setRunType(runType);
        run.setForced(forced);
        run.setReason(reason);
        run.setStatus(ReconciliationRunStatus.RUNNING);
        run.setStartedAt(now);
        return runRepository.save(run);
    }

    public record ReconciliationDailyRunResult(
        ReconciliationRunId runId,
        TenantId tenantId,
        LocalDate businessDate,
        String status,
        int anomalyCount,
        Instant startedAt,
        Instant completedAt,
        String reason,
        long checkedDrawCount,
        long checkedTicketCount,
        long criticalCount,
        long highCount,
        long mediumCount,
        long lowCount
    ) {}
}
