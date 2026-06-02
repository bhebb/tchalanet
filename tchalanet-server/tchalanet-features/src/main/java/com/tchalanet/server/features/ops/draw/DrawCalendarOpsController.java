package com.tchalanet.server.features.ops.draw;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowResult;
import com.tchalanet.server.core.draw.api.command.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.api.command.CloseDueDrawsResult;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeResult;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.api.command.OpenTodayDrawsCommand;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/platform/ops/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Scheduler")
public class DrawCalendarOpsController {

    private static final JobKey DRAW_GENERATE = JobKey.of("draw:lifecycle:generate");
    private static final JobKey DRAW_OPEN = JobKey.of("draw:lifecycle:open");
    private static final JobKey DRAW_CLOSE = JobKey.of("draw:lifecycle:close");
    private static final JobKey RESULTS_EXTERNAL_APPLY = JobKey.of("results:external:apply");

    private final CommandBus commandBus;
    private final BatchGate batchGate;
    private final TenantCatalog tenantCatalog;
    private final Clock clock;

    @Operation(summary = "Generate draws for a date range (ops)")
    @PostMapping("/generate")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_GENERATE,
        idExpression = "'draw-generate'",
        detailsExpression = "#req")
    public ApiResponse<GenerateDrawsBatchResponse> generate(@Valid @RequestBody GenerateDrawsRequest req) {
        // Target tenants: explicit tenantIds, else single tenantId (back-compat), else ALL active
        // tenants — mirroring the scheduled generateNext7Days job. Generation is idempotent, so a
        // full sweep only fills tenants that are still missing draws in the range.
        List<TenantId> targets = resolveTargetTenants(req);

        var outcomes = new ArrayList<GenerateDrawsBatchResponse.TenantOutcome>(targets.size());
        int created = 0, skipped = 0, alreadyExists = 0, conflicts = 0, providerClosed = 0;
        int succeeded = 0, failed = 0;

        for (TenantId tenantId : targets) {
            try {
                // Per-tenant gate + run in the TARGET tenant's RLS context: generate reads that
                // tenant's draw channels and INSERTs its draws, both tenant-scoped under RLS
                // (draw WITH CHECK requires tenant_id = current_tenant()). Running in the caller's
                // context would read the wrong channels and the INSERT would be rejected (500).
                batchGate.assertEnabledOrThrow(DRAW_GENERATE, tenantId);
                var res = TchContextScope.runWithTemporaryTenantResult(
                    tenantId.value(), "draw-generate",
                    () -> commandBus.execute(new GenerateDrawsForRangeCommand(
                        tenantId, req.from(), req.to(), req.dryRun(), req.force(), req.reason())));

                created += res.created();
                skipped += res.skipped();
                alreadyExists += res.alreadyExists();
                conflicts += res.conflicts();
                providerClosed += res.skippedProviderClosed();
                succeeded++;
                outcomes.add(new GenerateDrawsBatchResponse.TenantOutcome(
                    tenantId.value().toString(), true, res, null));
            } catch (Exception ex) {
                // One tenant's failure must not abort the rest (resilient batch, like the job).
                failed++;
                outcomes.add(new GenerateDrawsBatchResponse.TenantOutcome(
                    tenantId.value().toString(), false, null, ex.getMessage()));
            }
        }

        var totals = new GenerateDrawsForRangeResult(
            created, skipped, alreadyExists, conflicts, providerClosed);
        return ApiResponse.success(new GenerateDrawsBatchResponse(
            targets.size(), succeeded, failed, totals, outcomes));
    }

    private List<TenantId> resolveTargetTenants(GenerateDrawsRequest req) {
        if (req.tenantIds() != null && !req.tenantIds().isEmpty()) {
            return req.tenantIds().stream().map(TenantId::parse).toList();
        }
        if (req.tenantId() != null && !req.tenantId().isBlank()) {
            return List.of(TenantId.parse(req.tenantId()));
        }
        return tenantCatalog.listActiveTenantIds();
    }

    @Operation(summary = "Open today's scheduled draws (ops)")
    @PostMapping("/open-today")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_OPEN,
        idExpression = "'draw-open-today'",
        detailsExpression = "#req")
    public ApiResponse<OpenDueDrawsResult> openToday(@Valid @RequestBody OpenTodayDrawsRequest req) {
        batchGate.assertEnabledOrThrow(DRAW_OPEN, null);
        Instant now = req.now() == null ? clock.instant() : req.now();
        int limit = req.limit() == null ? 10000 : req.limit();
        var res = commandBus.execute(new OpenTodayDrawsCommand(
            now,
            req.drawDate(),
            limit,
            req.dryRun()));
        return ApiResponse.success(res);
    }

    @Operation(summary = "Close due draws (ops)")
    @PostMapping("/close-due")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_CLOSE,
        idExpression = "'draw-close-due'",
        detailsExpression = "#req")
    public ApiResponse<CloseDueDrawsResult> closeDue(@Valid @RequestBody CloseDueDrawsRequest req) {
        batchGate.assertEnabledOrThrow(DRAW_CLOSE, null);
        Instant now = req.now() == null ? clock.instant() : req.now();
        var res = commandBus.execute(new CloseDueDrawsCommand(now, req.limit(), req.dryRun()));
        return ApiResponse.success(res);
    }

    @Operation(summary = "Apply draw_result to draw.draw_result_id (slot-first)")
    @PostMapping("/apply")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_APPLY,
        idExpression = "#req.slotKeys() == null ? 'all-slots' : #req.slotKeys().toString()",
        detailsExpression = "#req")
    public ApiResponse<ApplyExternalResultsWindowResult> apply(@Valid @RequestBody ApplyExternalResultsRequest req,
                                                               @CurrentContext TchRequestContext ctx) {
        batchGate.assertEnabledOrThrow(RESULTS_EXTERNAL_APPLY, ctx.tenantId());
        var res =
            commandBus.execute(
                new ApplyExternalResultsWindowCommand(
                    ctx.tenantId(),
                    req.baseDate(),
                    req.daysBack(),
                    req.slotKeys(),
                    req.force(),
                    req.dryRun(),
                    req.maxSlots(),
                    req.reason()));
        return ApiResponse.success(res);
    }
}
