package com.tchalanet.server.features.ops.drawresult;

import com.tchalanet.server.common.context.web.CurrentContext;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.draw.api.command.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.core.drawresult.api.command.*;
import com.tchalanet.server.core.drawresult.api.query.GetDrawResultViewByIdQuery;
import com.tchalanet.server.core.drawresult.api.query.GetDrawResultViewBySlotQuery;
import com.tchalanet.server.core.drawresult.api.query.ListDrawResultsQuery;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import com.tchalanet.server.features.ops.drawresult.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/platform/ops/draw-results")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Ops • Draw Results")
@Validated
public class DrawResultsOpsController {

    private static final JobKey RESULTS_EXTERNAL_FETCH = JobKey.of("results:external:fetch");
    private static final JobKey RESULTS_EXTERNAL_REFRESH = JobKey.of("results:external:refresh");
    private static final JobKey RESULTS_EXTERNAL_OVERRIDE = JobKey.of("results:external:override");
    private static final JobKey RESULTS_EXTERNAL_MANUAL = JobKey.of("results:external:manual");

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final BatchGate gate;
    private final DrawResultOpsMapper mapper;

    @Operation(summary = "Fetch external results into draw_result (slot-first)")
    @PostMapping("/fetch")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_FETCH,
        idExpression = "#req.slotKeys() == null ? 'all-slots' : #req.slotKeys().toString()",
        detailsExpression = "#req")
    public ApiResponse<FetchExternalResultsWindowResult> fetch(
        @Valid @RequestBody FetchExternalResultsRequest req
    ) {
        gate.assertEnabledOrThrow(RESULTS_EXTERNAL_FETCH, null);

        var res = commandBus.execute(
            new FetchExternalResultsWindowCommand(
                null,
                nnDate(req.baseDate()),
                nnInt(req.daysBack(), 0),
                normalize(req.slotKeys()),
                req.force(),
                req.dryRun(),
                nnInt(req.maxSlots(), 200),
                req.reason(),
                req.includeRaw()
            )
        );

        return ApiResponse.success(res);
    }

    @Operation(summary = "Refresh external results (fetch then apply)")
    @PostMapping("/refresh")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_REFRESH,
        idExpression = "#req.slotKeys() == null ? 'all-slots' : #req.slotKeys().toString()",
        detailsExpression = "#req")
    public ApiResponse<RefreshExternalResultsWindowResult> refresh(
        @Valid @RequestBody FetchExternalResultsRequest req,
        @CurrentContext TchRequestContext ctx
    ) {
        gate.assertEnabledOrThrow(RESULTS_EXTERNAL_REFRESH, ctx.tenantId());

        var fetchRes = commandBus.execute(
            new FetchExternalResultsWindowCommand(
                null,
                nnDate(req.baseDate()),
                nnInt(req.daysBack(), 0),
                normalize(req.slotKeys()),
                req.force(),
                req.dryRun(),
                nnInt(req.maxSlots(), 200),
                req.reason(),
                req.includeRaw()
            )
        );

        var applyRes = commandBus.execute(
            new ApplyExternalResultsWindowCommand(
                ctx.tenantId(),
                nnDate(req.baseDate()),
                nnInt(req.daysBack(), 0),
                normalize(req.slotKeys()),
                req.force(),
                req.dryRun(),
                nnInt(req.maxSlots(), 200),
                req.reason()
            )
        );

        return ApiResponse.success(RefreshExternalResultsWindowResult.from(fetchRes, applyRes));
    }

    @Operation(summary = "Override a draw result for a slot")
    @PostMapping("/override")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_OVERRIDE,
        idExpression = "#result.data().drawResultId().value().toString()",
        detailsExpression = "#req")
    public ApiResponse<OverrideDrawResultResult> override(
        @Valid @RequestBody OverrideDrawResultRequest req
    ) {
        rejectFutureDate(req.drawDate());
        gate.assertEnabledOrThrow(RESULTS_EXTERNAL_OVERRIDE, null);

        var res = commandBus.execute(
            new OverrideDrawResultCommand(
                null,
                normalizeSlot(req.slotKey()),
                req.drawDate(),
                pick3FromLots(req.lot1()),
                pick4FromLots(req.lot2(), req.lot3()),
                req.reason(),
                req.force()
            )
        );

        return ApiResponse.success(res);
    }

    @Operation(summary = "Record a manual draw result")
    @PostMapping("/manual")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_MANUAL,
        idExpression = "#result.data().drawResultId().value().toString()",
        detailsExpression = "#req")
    public ApiResponse<RecordManualDrawResultResult> manual(
        @Valid @RequestBody RecordManualDrawResultRequest req
    ) {
        rejectFutureDate(req.drawDate());
        gate.assertEnabledOrThrow(RESULTS_EXTERNAL_MANUAL, null);

        var res = commandBus.execute(
            new RecordManualDrawResultCommand(
                null,
                req.drawDate(),
                normalizeSlot(req.slotKey()),
                req.recordedBy(),
                req.notes(),
                pick3FromLots(req.lot1()),
                pick4FromLots(req.lot2(), req.lot3()),
                req.force(),
                req.reason(),
                false // ops endpoint always writes CONFIRMED
            )
        );

        return ApiResponse.success(res);
    }


    @Operation(summary = "Confirm a PROVISIONAL draw result (platform review)")
    @PostMapping("/{drawResultId}/confirm")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_CONFIRM,
        idExpression = "#drawResultId.toString()",
        detailsExpression = "'confirmedBy:' + #ctx.externalSubject()")
    public ApiResponse<ConfirmDrawResultResult> confirm(
        @PathVariable DrawResultId drawResultId,
        @CurrentContext TchRequestContext ctx
    ) {
        var res = commandBus.execute(
            new ConfirmDrawResultCommand(drawResultId, ctx.externalSubject())
        );
        return ApiResponse.success(res);
    }

    @GetMapping
    public ApiResponse<TchPage<DrawResultOpsResponse>> search(
        @RequestParam(required = false) String slotKey,
        @RequestParam(required = false) DrawResultStatus status,
        @RequestParam(required = false) ResultQuality quality,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @TchPaging(
            allowedSort = {"occurredAt", "slotKey", "status", "quality"},
            defaultSort = {"occurredAt,DESC"}
        ) TchPageRequest pageReq
    ) {
        var page = queryBus.ask(new ListDrawResultsQuery(
            normalizeNullableSlot(slotKey),
            status,
            quality,
            from,
            to,
            pageReq.pageable()
        ));

        return ApiResponse.success(mapper.toPage(page));
    }

    @GetMapping("/{drawResultId}")
    public ApiResponse<DrawResultOpsResponse> getById(@PathVariable DrawResultId drawResultId) {
        var view = queryBus.ask(new GetDrawResultViewByIdQuery(drawResultId));
        return ApiResponse.success(mapper.toResponse(view));
    }

    @GetMapping("/by-slot")
    public ApiResponse<DrawResultOpsResponse> getBySlot(
        @RequestParam String slotKey,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredAt
    ) {
        var view = queryBus.ask(new GetDrawResultViewBySlotQuery(
            normalizeSlot(slotKey),
            occurredAt
        ));

        return ApiResponse.success(mapper.toResponse(view));
    }

    private static LocalDate nnDate(LocalDate d) {
        return d == null ? LocalDate.now() : d;
    }

    private static int nnInt(Integer v, int def) {
        return v == null ? def : v;
    }

    private static List<String> normalize(List<String> in) {
        if (in == null) {
            return List.of();
        }

        return in.stream()
            .filter(Objects::nonNull)
            .map(DrawResultsOpsController::normalizeSlot)
            .filter(s -> !s.isBlank())
            .distinct()
            .toList();
    }

    private static String normalizeSlot(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullableSlot(String s) {
        var normalized = normalizeSlot(s);
        return normalized.isBlank() ? null : normalized;
    }

    private static String pick3FromLots(String lot1) {
        return digits(lot1);
    }

    private static String pick4FromLots(String lot2, String lot3) {
        return digits(lot2) + digits(lot3);
    }

    private static String digits(String value) {
        return value == null ? "" : value.trim().replaceAll("\\D", "");
    }

    private static void rejectFutureDate(LocalDate drawDate) {
        if (drawDate != null && drawDate.isAfter(LocalDate.now())) {
            throw ProblemRest.badRequest("draw result date cannot be in the future");
        }
    }

}
