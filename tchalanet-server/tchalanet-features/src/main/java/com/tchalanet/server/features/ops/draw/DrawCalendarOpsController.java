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
import java.util.function.Function;

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
    public ApiResponse<TenantBatchResponse<GenerateDrawsForRangeResult>> generate(
            @Valid @RequestBody GenerateDrawsRequest req) {
        // Target tenants: explicit tenantIds, else single tenantId (back-compat), else ALL active
        // tenants — mirroring the scheduled generateNext7Days job. Generation is idempotent, so a
        // full sweep only fills tenants that are still missing draws in the range.
        return ApiResponse.success(runPerTenant(
            req.tenantIds(), req.tenantId(), DRAW_GENERATE, "draw-generate",
            tenantId -> commandBus.execute(new GenerateDrawsForRangeCommand(
                tenantId, req.from(), req.to(), req.dryRun(), req.force(), req.reason()))));
    }

    /**
     * Run {@code work} once per target tenant — each in that tenant's RLS context — collecting a
     * per-tenant outcome. The draw queries/writes are tenant-scoped (draw WITH CHECK requires
     * {@code tenant_id = current_tenant()}), so running in the caller's context would touch the
     * wrong tenant or be rejected by RLS. A single tenant's failure does not abort the rest.
     */
    private <R> TenantBatchResponse<R> runPerTenant(
            List<String> explicitTenantIds, String singleTenantId,
            JobKey gate, String label, Function<TenantId, R> work) {
        List<TenantId> targets = resolveTargetTenants(explicitTenantIds, singleTenantId);
        var outcomes = new ArrayList<TenantBatchResponse.Outcome<R>>(targets.size());
        int succeeded = 0, failed = 0;
        for (TenantId tenantId : targets) {
            try {
                batchGate.assertEnabledOrThrow(gate, tenantId);
                R res = TchContextScope.runWithTemporaryTenantResult(
                    tenantId.value(), label, () -> work.apply(tenantId));
                outcomes.add(new TenantBatchResponse.Outcome<>(tenantId.value().toString(), true, res, null));
                succeeded++;
            } catch (Exception ex) {
                outcomes.add(new TenantBatchResponse.Outcome<>(tenantId.value().toString(), false, null, ex.getMessage()));
                failed++;
            }
        }
        return new TenantBatchResponse<>(targets.size(), succeeded, failed, outcomes);
    }

    private List<TenantId> resolveTargetTenants(List<String> explicitTenantIds, String singleTenantId) {
        if (explicitTenantIds != null && !explicitTenantIds.isEmpty()) {
            return explicitTenantIds.stream().map(TenantId::parse).toList();
        }
        if (singleTenantId != null && !singleTenantId.isBlank()) {
            return List.of(TenantId.parse(singleTenantId));
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
    public ApiResponse<TenantBatchResponse<OpenDueDrawsResult>> openToday(
            @Valid @RequestBody OpenTodayDrawsRequest req) {
        // Optional tenant list, else ALL active tenants — each opened in its own RLS context
        // (mirrors the scheduled openToday job).
        Instant now = req.now() == null ? clock.instant() : req.now();
        int limit = req.limit() == null ? 10000 : req.limit();
        return ApiResponse.success(runPerTenant(
            req.tenantIds(), null, DRAW_OPEN, "draw-open-today",
            tenantId -> commandBus.execute(new OpenTodayDrawsCommand(
                now, req.drawDate(), limit, req.dryRun()))));
    }

    @Operation(summary = "Close due draws (ops)")
    @PostMapping("/close-due")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_CLOSE,
        idExpression = "'draw-close-due'",
        detailsExpression = "#req")
    public ApiResponse<TenantBatchResponse<CloseDueDrawsResult>> closeDue(
            @Valid @RequestBody CloseDueDrawsRequest req) {
        // Optional tenant list, else ALL active tenants — each closed in its own RLS context.
        Instant now = req.now() == null ? clock.instant() : req.now();
        return ApiResponse.success(runPerTenant(
            req.tenantIds(), null, DRAW_CLOSE, "draw-close-due",
            tenantId -> commandBus.execute(new CloseDueDrawsCommand(now, req.limit(), req.dryRun()))));
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
