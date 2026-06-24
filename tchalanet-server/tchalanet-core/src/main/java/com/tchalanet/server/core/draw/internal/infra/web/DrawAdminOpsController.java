package com.tchalanet.server.core.draw.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.core.draw.api.command.*;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.internal.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.internal.infra.web.model.*;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Draws • Admin")
public class DrawAdminOpsController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final DrawAdminWebMapper mapper;


    @Operation(summary = "Correct an already applied draw result")
    @PostMapping("/{drawId}/results/correct")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_CORRECT_APPLIED_RESULT,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> correctAppliedDrawResult(
        @PathVariable DrawId drawId,
        @RequestBody @Valid CorrectAppliedDrawResultRequest request) {

        commandBus.execute(new CorrectAppliedDrawResultCommand(
            drawId,
            request.correctedDrawResultId(),
            request.reason(),
            request.idempotencyKey(),
            request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Cancel a draw")
    @PostMapping("/{drawId}/cancel")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_CANCEL,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> cancel(
        @PathVariable DrawId drawId,
        @RequestBody @Valid CancelDrawRequest request) {

        commandBus.execute(new CancelDrawCommand(drawId, request.reasonCode(), request.reasonLabel(), request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Reschedule a draw")
    @PostMapping("/{drawId}/reschedule")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_RESCHEDULE,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> reschedule(
        @PathVariable DrawId drawId,
        @RequestBody @Valid RescheduleDrawRequest request) {

        if (!request.cutoffAt().isBefore(request.scheduledAt())) {
            throw ProblemRest.badRequest("draw.schedule_invalid");
        }

        commandBus.execute(new RescheduleDrawCommand(
            drawId,
            request.scheduledAt(),
            request.cutoffAt(),
            request.reason(),
            request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Lock a draw")
    @PostMapping("/{drawId}/lock")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_LOCK,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> lock(
        @PathVariable DrawId drawId,
        @RequestBody @Valid LockDrawRequest request) {

        commandBus.execute(new LockDrawCommand(drawId, request.reason()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Unlock a draw")
    @PostMapping("/{drawId}/unlock")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_UNLOCK,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> unlock(
        @PathVariable DrawId drawId,
        @RequestBody @Valid UnlockDrawRequest request) {

        commandBus.execute(new UnlockDrawCommand(drawId, request.reason()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Archive a draw")
    @PostMapping("/{drawId}/archive")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_ARCHIVE,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> archive(
        @PathVariable DrawId drawId,
        @RequestBody @Valid ArchiveDrawRequest request) {

        commandBus.execute(new ArchiveDrawCommand(drawId, request.reason(), request.force()));

        return ApiResponse.success(reload(drawId));
    }

    /**
     * Settlement functionality is deferred pending core.sales alignment.
     * This endpoint is marked for ops/admin strict use only.
     * DO NOT use in production until settlement is properly aligned with sales/payout/ledger.
     */
    @Operation(
        summary = "Settle a draw (DEFERRED - ops/admin strict use only)",
        description = "Settlement pending core.sales alignment. Use with extreme caution.")
    @PostMapping("/{drawId}/settle")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_SETTLE,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> settle(
        @PathVariable DrawId drawId,
        @RequestBody @Valid SettleDrawRequest request) {

        commandBus.execute(new SettleDrawCommand(drawId, request.reason(), request.force()));

        return ApiResponse.success(reload(drawId));
    }

    @Operation(summary = "Record a manual draw result (TENANT_ADMIN+)")
    @PostMapping("/{drawId}/manual-result")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_MANUAL,
        idExpression = "#drawId.value().toString()",
        detailsExpression = "#request")
    public ApiResponse<DrawSummaryResponse> manualResult(
        @PathVariable DrawId drawId,
        @RequestBody @Valid AdminDrawManualResultRequest request,
        @CurrentContext TchRequestContext ctx) {

        var draw = queryBus.ask(new GetDrawByIdQuery(drawId));
        var recordedBy = request.recordedBy() != null ? request.recordedBy() : ctx.externalSubject();

        commandBus.execute(new RecordManualDrawResultCommand(
            draw.tenantId(),
            draw.drawDate(),
            draw.resultSlotKey(),
            recordedBy,
            request.notes(),
            request.pick3(),
            request.pick4(),
            request.force(),
            request.reason(),
            request.observeTrustPolicy()));

        return ApiResponse.success(reload(drawId));
    }

    private DrawSummaryResponse reload(DrawId drawId) {
        var summary = queryBus.ask(new GetDrawByIdQuery(drawId));
        return mapper.toDrawSummaryResponse(summary);
    }
}
