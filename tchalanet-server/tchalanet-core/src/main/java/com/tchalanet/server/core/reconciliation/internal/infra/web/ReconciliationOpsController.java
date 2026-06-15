package com.tchalanet.server.core.reconciliation.internal.infra.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.core.reconciliation.internal.application.ReconciliationDailyRunService;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationAnomalyJpaEntity;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationAnomalyJpaRepository;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationRunJpaEntity;
import com.tchalanet.server.core.reconciliation.internal.infra.persistence.ReconciliationRunJpaRepository;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/reconciliation")
@RequiredArgsConstructor
@Validated
public class ReconciliationOpsController {
    private final ReconciliationDailyRunService reconciliationDailyRunService;
    private final ReconciliationRunJpaRepository runRepository;
    private final ReconciliationAnomalyJpaRepository anomalyRepository;

    @PostMapping("/daily-runs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @AuditLog(
        action = AuditAction.FORCE_OPERATION,
        entity = AuditEntityType.BATCH_JOB,
        detailsExpression = "{ 'job': 'daily_reconciliation', 'businessDate': #request.businessDate(), 'reason': #request.reason() }"
    )
    public ResponseEntity<ReconciliationRunResponse> startDailyRun(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody ReconciliationRunRequest request
    ) {
        var result = reconciliationDailyRunService.forceDailyRun(
            ctx.effectiveTenantIdRequired(),
            request.businessDate(),
            request.reason()
        );
        return ResponseEntity.ok(new ReconciliationRunResponse(
            result.runId().toString(),
            result.status(),
            result.businessDate(),
            result.anomalyCount(),
            result.startedAt(),
            result.completedAt(),
            result.checkedDrawCount(),
            result.checkedTicketCount(),
            result.criticalCount(),
            result.highCount(),
            result.mediumCount(),
            result.lowCount()
        ));
    }

    @GetMapping("/daily-runs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ReconciliationRunResponse>> listDailyRuns(
        @CurrentContext TchRequestContext ctx
    ) {
        var tenantId = ctx.effectiveTenantIdRequired().value();
        return ResponseEntity.ok(runRepository.findByTenantIdOrderByStartedAtDesc(tenantId)
            .stream()
            .map(this::toRunResponse)
            .toList());
    }

    @GetMapping("/daily-runs/{runId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ReconciliationRunDetailResponse> getDailyRun(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UUID runId
    ) {
        var tenantId = ctx.effectiveTenantIdRequired().value();
        var run = runRepository.findByTenantIdAndId(tenantId, runId)
            .orElseThrow(() -> new IllegalArgumentException("reconciliation.run_not_found"));
        var anomalies = anomalyRepository.findByTenantIdAndRunIdOrderBySeverityAscAnomalyTypeAsc(tenantId, runId)
            .stream()
            .map(this::toAnomalyResponse)
            .toList();
        return ResponseEntity.ok(new ReconciliationRunDetailResponse(toRunResponse(run), anomalies));
    }

    @GetMapping("/daily-runs/{runId}/anomalies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ReconciliationAnomalyResponse>> listDailyRunAnomalies(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UUID runId
    ) {
        var tenantId = ctx.effectiveTenantIdRequired().value();
        return ResponseEntity.ok(anomalyRepository.findByTenantIdAndRunIdOrderBySeverityAscAnomalyTypeAsc(tenantId, runId)
            .stream()
            .map(this::toAnomalyResponse)
            .toList());
    }

    private ReconciliationRunResponse toRunResponse(ReconciliationRunJpaEntity run) {
        return new ReconciliationRunResponse(
            run.getId().toString(),
            run.getStatus().name(),
            run.getBusinessDate(),
            (int) run.getAnomalyCount(),
            run.getStartedAt(),
            run.getCompletedAt(),
            run.getCheckedDrawCount(),
            run.getCheckedTicketCount(),
            run.getCriticalCount(),
            run.getHighCount(),
            run.getMediumCount(),
            run.getLowCount()
        );
    }

    private ReconciliationAnomalyResponse toAnomalyResponse(ReconciliationAnomalyJpaEntity anomaly) {
        return new ReconciliationAnomalyResponse(
            anomaly.getId(),
            anomaly.getRunId(),
            anomaly.getBusinessDate(),
            anomaly.getSeverity().name(),
            anomaly.getAnomalyType().name(),
            anomaly.getStatus().name(),
            anomaly.getFingerprint(),
            anomaly.getDrawId(),
            anomaly.getDrawChannelId(),
            anomaly.getDrawResultId(),
            anomaly.getTicketId(),
            anomaly.getTicketCode(),
            anomaly.getPublicCode(),
            anomaly.getDisplayCode(),
            anomaly.getPayoutClaimId(),
            anomaly.getPayoutPaymentId(),
            anomaly.getExpectedStatus(),
            anomaly.getActualStatus(),
            anomaly.getExpectedAmount(),
            anomaly.getActualAmount(),
            anomaly.getCurrency(),
            anomaly.getMessage(),
            anomaly.getFirstSeenAt(),
            anomaly.getLastSeenAt(),
            anomaly.getResolvedAt()
        );
    }

    public record ReconciliationRunRequest(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
        @NotBlank
        String reason
    ) {}

    public record ReconciliationRunResponse(
        String runId,
        String status,
        LocalDate businessDate,
        int anomalyCount,
        Instant startedAt,
        Instant completedAt,
        long checkedDrawCount,
        long checkedTicketCount,
        long criticalCount,
        long highCount,
        long mediumCount,
        long lowCount
    ) {}

    public record ReconciliationRunDetailResponse(
        ReconciliationRunResponse run,
        List<ReconciliationAnomalyResponse> anomalies
    ) {}

    public record ReconciliationAnomalyResponse(
        UUID anomalyId,
        UUID runId,
        LocalDate businessDate,
        String severity,
        String anomalyType,
        String status,
        String fingerprint,
        UUID drawId,
        UUID drawChannelId,
        UUID drawResultId,
        UUID ticketId,
        String ticketCode,
        String publicCode,
        String displayCode,
        UUID payoutClaimId,
        UUID payoutPaymentId,
        String expectedStatus,
        String actualStatus,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        String currency,
        String message,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant resolvedAt
    ) {}
}
