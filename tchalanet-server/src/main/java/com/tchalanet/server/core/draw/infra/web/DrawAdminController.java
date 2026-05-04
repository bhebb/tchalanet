package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.draw.application.command.model.*;
import com.tchalanet.server.core.draw.application.query.model.*;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.infra.web.model.*;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin • Draws")
public class DrawAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final DrawAdminWebMapper mapper;
    private final Clock clock;

    @Operation(summary = "List draws")
    @GetMapping
    public ApiResponse<TchPage<DrawSummaryResponse>> listDraws(
        @Valid DrawSearchRequest request,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "cutoffAt", "status", "createdAt"},
            defaultSort = {"scheduledAt,desc"})
        TchPageRequest pageReq) {

        var criteria = DrawSearchCriteria.of(
            request.resultSlotId(),
            request.status(),
            request.from(),
            request.to(),
            clock);

        TchPage<DrawSummary> page = queryBus.send(new ListDrawsQuery(criteria, pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "List today's draws")
    @GetMapping("/today")
    public ApiResponse<TchPage<DrawSummaryResponse>> todayDraws(
        @RequestParam(required = false) ResultSlotId resultSlotId,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "cutoffAt", "status"},
            defaultSort = {"scheduledAt,asc"})
        TchPageRequest pageReq) {

        var criteria = DrawSearchCriteria.today(resultSlotId, clock);
        TchPage<DrawSummary> page = queryBus.send(new ListDrawsQuery(criteria, pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "List upcoming draws")
    @GetMapping("/upcoming")
    public ApiResponse<TchPage<DrawSummaryResponse>> upcomingDraws(
        @RequestParam(required = false) ResultSlotId resultSlotId,
        @RequestParam(defaultValue = "7") int days,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "cutoffAt", "status"},
            defaultSort = {"scheduledAt,asc"})
        TchPageRequest pageReq) {

        if (days < 1 || days > 30) {
            throw ProblemRest.badRequest("draw.lookahead_days_invalid");
        }

        var criteria = DrawSearchCriteria.upcoming(resultSlotId, days, clock);
        TchPage<DrawSummary> page = queryBus.send(new ListDrawsQuery(criteria, pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "List next draws")
    @GetMapping("/next")
    public ApiResponse<TchPage<DrawSummaryResponse>> nextDraws(
        @RequestParam(required = false) ResultSlotId resultSlotId,
        @RequestParam(defaultValue = "24") int lookaheadHours,
        @RequestParam(defaultValue = "1") int limitPerChannel,
        @TchPaging(
            allowedSort = {"scheduledAt", "cutoffAt", "drawDate"},
            defaultSort = {"scheduledAt,asc"})
        TchPageRequest pageReq) {

        TchPage<DrawSummary> page = queryBus.send(new ListNextDrawsQuery(
            resultSlotId,
            lookaheadHours,
            limitPerChannel,
            pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "List latest draws with applied results")
    @GetMapping("/latest-with-results")
    public ApiResponse<TchPage<DrawSummaryResponse>> latestWithResults(
        @RequestParam(required = false) List<String> resultSlotKeys,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "resultOccurredAt"},
            defaultSort = {"drawDate,desc", "scheduledAt,desc"})
        TchPageRequest pageReq) {

        TchPage<DrawSummary> page = queryBus.send(new ListLatestDrawsWithResultsQuery(
            resultSlotKeys,
            pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "Get draw by id")
    @GetMapping("/{drawId}")
    public ApiResponse<DrawSummaryResponse> getDraw(
        @PathVariable DrawId drawId) {

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Get draw result details")
    @GetMapping("/{drawId}/results")
    public ApiResponse<DrawResultsResponse> getDrawResults(
        @PathVariable DrawId drawId) {

        DrawResultView result = queryBus.send(new GetDrawResultsQuery(drawId));

        return ApiResponse.success(mapper.toDrawResultsResponse(result));
    }

    @Operation(summary = "Correct an already applied draw result")
    @PostMapping("/{drawId}/results/correct")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_CORRECT_APPLIED_RESULT,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> correctAppliedDrawResult(
        @PathVariable DrawId drawId,
        @RequestBody @Valid CorrectAppliedDrawResultRequest request) {

        commandBus.send(new CorrectAppliedDrawResultCommand(
            drawId,
            request.correctedDrawResultId(),
            request.reason(),
            request.idempotencyKey(),
            request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Cancel a draw")
    @PostMapping("/{drawId}/cancel")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_CANCEL,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> cancel(
        @PathVariable DrawId drawId,
        @RequestBody @Valid CancelDrawRequest request) {

        commandBus.send(new CancelDrawCommand(drawId, request.reason(), request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Reschedule a draw")
    @PostMapping("/{drawId}/reschedule")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_RESCHEDULE,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> reschedule(
        @PathVariable DrawId drawId,
        @RequestBody @Valid RescheduleDrawRequest request) {

        if (!request.scheduledAt().isBefore(request.cutoffAt())) {
            throw ProblemRest.badRequest("draw.schedule_invalid");
        }

        commandBus.send(new RescheduleDrawCommand(
            drawId,
            request.scheduledAt(),
            request.cutoffAt(),
            request.reason(),
            request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Lock a draw")
    @PostMapping("/{drawId}/lock")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_LOCK,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> lock(
        @PathVariable DrawId drawId,
        @RequestBody @Valid LockDrawRequest request) {

        commandBus.send(new LockDrawCommand(drawId, request.reason()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Unlock a draw")
    @PostMapping("/{drawId}/unlock")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_UNLOCK,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> unlock(
        @PathVariable DrawId drawId,
        @RequestBody @Valid UnlockDrawRequest request) {

        commandBus.send(new UnlockDrawCommand(drawId, request.reason()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Archive a draw")
    @PostMapping("/{drawId}/archive")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_ARCHIVE,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> archive(
        @PathVariable DrawId drawId,
        @RequestBody @Valid ArchiveDrawRequest request) {

        commandBus.send(new ArchiveDrawCommand(drawId, request.reason(), request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Override draw fields")
    @PostMapping("/{drawId}/override")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_OVERRIDE,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> overrideDraw(
        @PathVariable DrawId drawId,
        @RequestBody @Valid OverrideDrawRequest request) {

        commandBus.send(new OverrideDrawCommand(
            drawId,
            request.status(),
            request.scheduledAt(),
            request.cutoffAt(),
            request.reason(),
            request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Settle a draw")
    @PostMapping("/{drawId}/settle")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_SETTLE,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> settle(
        @PathVariable DrawId drawId,
        @RequestBody @Valid SettleDrawRequest request) {

        commandBus.send(new SettleDrawCommand(drawId, request.reason(), request.force()));

        return ApiResponse.success(reload(drawId));
    }

    private DrawSummaryResponse reload(DrawId drawId) {
        var summary = queryBus.send(new GetDrawByIdQuery(drawId));
        return mapper.toDrawSummaryResponse(summary);
    }
}
