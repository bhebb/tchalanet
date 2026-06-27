package com.tchalanet.server.features.ops.draw;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.ListDrawsQuery;
import com.tchalanet.server.core.draw.api.query.DrawResultSummary;
import com.tchalanet.server.features.ops.batch.OpsBatchLaunchFacade;
import com.tchalanet.server.features.ops.batch.model.OpsLaunchResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private final QueryBus queryBus;
    private final OpsBatchLaunchFacade batchLaunchFacade;

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
    public ApiResponse<OpsLaunchResponse> generate(
        @Valid @RequestBody GenerateDrawsRequest req) {
        return ApiResponse.success(batchLaunchFacade.launchForTenants(
            DRAW_GENERATE,
            req.tenantCodes(),
            tenantId -> params(
                "from", req.from().toString(),
                "to", req.to().toString(),
                "dry_run", Boolean.toString(req.dryRun()),
                "force", Boolean.toString(req.force()),
                "reason", req.reason())));
    }

    @Operation(summary = "Open today's scheduled draws (ops)")
    @PostMapping("/open-today")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_OPEN,
        idExpression = "'draw-open-today'",
        detailsExpression = "#req")
    public ApiResponse<OpsLaunchResponse> openToday(
        @Valid @RequestBody OpenTodayDrawsRequest req) {
        return ApiResponse.success(batchLaunchFacade.launchForTenants(
            DRAW_OPEN,
            req.tenantCodes(),
            tenantId -> params(
                "date", req.drawDate() == null ? null : req.drawDate().toString(),
                "max_items", req.limit() == null ? null : req.limit().toString(),
                "dry_run", Boolean.toString(req.dryRun()))));
    }

    @Operation(summary = "Close due draws (ops)")
    @PostMapping("/close-due")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_CLOSE,
        idExpression = "'draw-close-due'",
        detailsExpression = "#req")
    public ApiResponse<OpsLaunchResponse> closeDue(
        @Valid @RequestBody CloseDueDrawsRequest req) {
        return ApiResponse.success(batchLaunchFacade.launchForTenants(
            DRAW_CLOSE,
            req.tenantCodes(),
            tenantId -> params(
                "max_items", Integer.toString(req.limit()),
                "dry_run", Boolean.toString(req.dryRun()))));
    }

    @Operation(summary = "Apply draw_result to draw.draw_result_id (slot-first)")
    @PostMapping("/apply")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_APPLY,
        idExpression = "#req.slotKeys() == null ? 'all-slots' : #req.slotKeys().toString()",
        detailsExpression = "#req")
    public ApiResponse<OpsLaunchResponse> apply(@Valid @RequestBody ApplyExternalResultsRequest req) {
        return ApiResponse.success(batchLaunchFacade.launchForTenants(
            RESULTS_EXTERNAL_APPLY,
            req.tenantCodes(),
            tenantId -> params(
                "date", req.baseDate() == null ? null : req.baseDate().toString(),
                "days_back", req.daysBack() == null ? null : req.daysBack().toString(),
                "slot_keys", join(req.slotKeys()),
                "max_slots", req.maxSlots() == null ? null : req.maxSlots().toString(),
                "dry_run", Boolean.toString(req.dryRun()),
                "force", Boolean.toString(req.force()),
                "reason", req.reason())));
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

    private static Map<String, String> params(String... entries) {
        var params = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            if (entries[i + 1] != null && !entries[i + 1].isBlank()) {
                params.put(entries[i], entries[i + 1]);
            }
        }
        return params;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }
}
