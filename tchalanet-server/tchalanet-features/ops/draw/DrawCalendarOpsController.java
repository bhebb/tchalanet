package com.tchalanet.server.features.ops.draw;

import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.draw.application.command.model.*;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.Clock;

@RestController
@RequestMapping("/platform/ops/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Scheduler")
public class DrawCalendarOpsController {

    private final CommandBus commandBus;
    private final BatchGate batchGate;
    private final Clock clock;
    private final DrawProperties drawProperties;

    @Operation(summary = "Generate draws for a date range (ops)")
    @PostMapping("/generate")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_GENERATE,
        idExpression = "#req.tenantId()",
        detailsExpression = "#req")
    public ApiResponse<GenerateDrawsForRangeResult> generate(@Valid @RequestBody GenerateDrawsRequest req) {
        batchGate.assertEnabledOrThrow(BatchJobKeys.DRAW_GENERATE, TenantId.parse(req.tenantId()));
        var res = commandBus.execute(
            new GenerateDrawsForRangeCommand(
                TenantId.parse(req.tenantId()), req.from(), req.to(), req.dryRun(), req.force(), req.reason()));
        return ApiResponse.success(res);
    }

    @Operation(summary = "Open due draws (ops)")
    @PostMapping("/open-due")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_OPEN,
        idExpression = "'draw-open-due'",
        detailsExpression = "#req")
    public ApiResponse<OpenDueDrawsResult> openDue(@Valid @RequestBody OpenDueDrawsRequest req) {
        batchGate.assertEnabledOrThrow(BatchJobKeys.DRAW_OPEN, null);
        Instant now = req.now() == null ? clock.instant() : req.now();
        var res = commandBus.execute(
            new OpenDueDrawsCommand(
                now, req.limit(), req.openHorizonHours(), req.openLagHours(), req.dryRun()));
        return ApiResponse.success(res);
    }

    @Operation(summary = "Open today's scheduled draws (ops)")
    @PostMapping("/open-today")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_OPEN,
        idExpression = "'draw-open-today'",
        detailsExpression = "#req")
    public ApiResponse<OpenDueDrawsResult> openToday(@Valid @RequestBody OpenTodayDrawsRequest req) {
        batchGate.assertEnabledOrThrow(BatchJobKeys.DRAW_OPEN, null);
        Instant now = req.now() == null ? clock.instant() : req.now();
        int limit = req.limit() == null ? 10000 : req.limit();
        var res = commandBus.execute(new OpenTodayDrawsCommand(
            now,
            req.drawDate(),
            drawProperties.getScheduler().getOpenToday().getDefaultSalesOpenTime(),
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
        batchGate.assertEnabledOrThrow(BatchJobKeys.DRAW_CLOSE, null);
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
        batchGate.assertEnabledOrThrow(BatchJobKeys.RESULTS_EXTERNAL_APPLY, ctx.tenantId());
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
