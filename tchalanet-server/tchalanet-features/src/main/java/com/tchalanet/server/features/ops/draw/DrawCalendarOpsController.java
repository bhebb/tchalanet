package com.tchalanet.server.features.ops.draw;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowResult;
import com.tchalanet.server.core.draw.api.command.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.api.command.CloseDueDrawsResult;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeResult;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.api.command.OpenTodayDrawsCommand;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.ListDrawsQuery;
import com.tchalanet.server.core.draw.api.query.DrawResultSummary;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.tchalanet.server.common.bus.QueryBus;

@RestController
@RequestMapping("/platform/ops/draws")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Ops • Scheduler")
public class DrawCalendarOpsController {

    private static final JobKey DRAW_GENERATE = JobKey.of("draw:lifecycle:generate");
    private static final JobKey DRAW_OPEN = JobKey.of("draw:lifecycle:open");
    private static final JobKey DRAW_CLOSE = JobKey.of("draw:lifecycle:close");
    private static final JobKey RESULTS_EXTERNAL_APPLY = JobKey.of("results:external:apply");

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final BatchGate batchGate;
    private final TenantPreContextLookupApi tenantPreContextLookupApi;
    private final Clock clock;

    @Operation(summary = "List draws across visible tenants (ops)")
    @GetMapping
    public ApiResponse<TchPage<DrawOpsResponse>> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String resultSlotKey,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "status", "resultSlotKey"},
            defaultSort = {"drawDate,DESC", "scheduledAt,DESC"}
        ) TchPageRequest pageReq
    ) {
        var criteria = DrawSearchCriteria.of(null, status, from, to)
            .withResultSlotKey(resultSlotKey);
        var page = queryBus.ask(new ListDrawsQuery(criteria, pageReq.pageable()));
        return ApiResponse.success(TchPageMapper.map(page, this::toResponse));
    }

    @Operation(summary = "Generate draws for a date range (ops)")
    @PostMapping("/generate")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_GENERATE,
        idExpression = "'draw-generate'",
        detailsExpression = "#req")
    public ApiResponse<TenantBatchResponse<GenerateDrawsForRangeResult>> generate(
        @Valid @RequestBody GenerateDrawsRequest req) {
        // Target tenants: the listed tenant codes, else ALL active tenants — mirroring the
        // scheduled generateNext7Days job. Generation is idempotent, so a full sweep only fills
        // tenants that are still missing draws in the range.
        return ApiResponse.success(runPerTenant(
            req.tenantCodes(), DRAW_GENERATE, "draw-generate",
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
        List<String> tenantCodes, JobKey gate, String label, Function<TenantId, R> work) {
        // The gate gates the operation, not an individual tenant: check it once up front (like the
        // scheduler) so a disabled gate surfaces clearly instead of being swallowed as N per-tenant
        // failures inside the loop.
        batchGate.assertEnabledOrThrow(gate, null);

        List<TenantId> targets = resolveTargetTenants(tenantCodes);
        var outcomes = new ArrayList<TenantBatchResponse.Outcome<R>>(targets.size());
        int succeeded = 0, failed = 0;
        for (TenantId tenantId : targets) {
            try {
                R res = TchContextScope.runWithTemporaryTenantResult(
                    tenantId.value(), label, () -> work.apply(tenantId));
                outcomes.add(new TenantBatchResponse.Outcome<>(tenantId.value().toString(), true, res, null));
                succeeded++;
            } catch (Exception ex) {
                outcomes.add(new TenantBatchResponse.Outcome<>(tenantId.value().toString(), false, null, ex.getMessage()));
                failed++;
            }
        }
        // A single tenant's failure must not abort the rest, but a sweep where EVERY tenant failed
        // (incl. the single-tenant case) is a real error — surface it instead of a 200 that masks it.
        if (succeeded == 0 && failed > 0) {
            throw ProblemRest.of(HttpStatus.INTERNAL_SERVER_ERROR,
                "Draw " + label + " failed for all " + failed + " tenant(s); first error: "
                    + outcomes.get(0).error());
        }
        return new TenantBatchResponse<>(targets.size(), succeeded, failed, outcomes);
    }

    private List<TenantId> resolveTargetTenants(List<String> tenantCodes) {
        if (tenantCodes == null || tenantCodes.isEmpty()) {
            return tenantPreContextLookupApi.listActiveTenantIds();
        }
        return tenantCodes.stream()
            .filter(code -> code != null && !code.isBlank())
            .map(code -> tenantPreContextLookupApi.findByCode(code.trim().toLowerCase())
                .map(TenantContextLookupView::tenantId)
                .orElseThrow(() -> ProblemRest.badRequest("Unknown tenant code: " + code)))
            .collect(Collectors.toList());
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
            req.tenantCodes(), DRAW_OPEN, "draw-open-today",
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
            req.tenantCodes(), DRAW_CLOSE, "draw-close-due",
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

    private DrawOpsResponse toResponse(DrawSummary draw) {
        var result = toResult(draw.result());
        return new DrawOpsResponse(
            draw.drawId(),
            draw.tenantId().value().toString(),
            new DrawOpsResponse.Channel(
                draw.drawChannelId().value().toString(),
                draw.drawChannelCode(),
                draw.drawChannelLabel()),
            new DrawOpsResponse.Slot(
                draw.resultSlotId().value().toString(),
                draw.resultSlotKey(),
                draw.resultSlotKey(),
                draw.resultTimezone(),
                draw.resultDrawTime() == null ? null : draw.resultDrawTime().toString()),
            draw.drawDate(),
            draw.scheduledAt(),
            draw.cutoffAt(),
            draw.status(),
            draw.drawChannelActive(),
            result);
    }

    private DrawOpsResponse.Result toResult(DrawResultSummary result) {
        if (result == null) {
            return null;
        }

        return new DrawOpsResponse.Result(
            result.id().value().toString(),
            result.occurredAt(),
            result.status().name(),
            haitiLot(result, "lot1"),
            haitiLot(result, "lot2"),
            haitiLot(result, "lot3"),
            haitiLot(result, "lot4"));
    }

    private static String haitiLot(DrawResultSummary result, String field) {
        var haiti = result.haitiResult();
        if (haiti == null) {
            return null;
        }
        var value = haiti.get(field);
        return value == null ? null : value.toString();
    }
}
