package com.tchalanet.server.core.draw.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.draw.api.command.CorrectAppliedDrawResultCommand;
import com.tchalanet.server.core.draw.api.command.RescheduleDrawCommand;
import com.tchalanet.server.core.draw.api.error.DrawErrorCodes;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.internal.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.internal.infra.web.model.AdminDrawManualResultRequest;
import com.tchalanet.server.core.draw.internal.infra.web.model.CorrectAppliedDrawResultRequest;
import com.tchalanet.server.core.draw.internal.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.internal.infra.web.model.RescheduleDrawRequest;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasPermission(null, 'draw.read')")
@Tag(name = "Draws • Admin")
public class DrawAdminOpsController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final DrawAdminWebMapper mapper;

  @Operation(summary = "Correct an already applied draw result")
  @PostMapping("/{drawId}/results/correct")
  @PreAuthorize("hasRole('SUPER_ADMIN') and hasPermission(null, 'draw_result.override')")
  @AuditLog(
      entity = AuditEntityType.DRAW,
      action = AuditAction.DRAW_CORRECT_APPLIED_RESULT,
      idExpression = "#drawId.value().toString()",
      detailsExpression = "#request")
  public ApiResponse<DrawSummaryResponse> correctAppliedDrawResult(
      @PathVariable DrawId drawId, @RequestBody @Valid CorrectAppliedDrawResultRequest request) {

    commandBus.execute(
        new CorrectAppliedDrawResultCommand(
            drawId,
            request.correctedDrawResultId(),
            request.reason(),
            request.idempotencyKey(),
            request.force()));

    return ApiResponse.success(reload(drawId));
  }

  @Operation(summary = "Reschedule a draw")
  @PostMapping("/{drawId}/reschedule")
  @PreAuthorize("hasPermission(null, 'draw.schedule.manage')")
  @AuditLog(
      entity = AuditEntityType.DRAW,
      action = AuditAction.DRAW_RESCHEDULE,
      idExpression = "#drawId.value().toString()",
      detailsExpression = "#request")
  public ApiResponse<DrawSummaryResponse> reschedule(
      @PathVariable DrawId drawId, @RequestBody @Valid RescheduleDrawRequest request) {

    if (!request.cutoffAt().isBefore(request.scheduledAt())) {
      throw ProblemRest.of(DrawErrorCodes.SCHEDULE_INVALID);
    }

    commandBus.execute(
        new RescheduleDrawCommand(
            drawId, request.scheduledAt(), request.cutoffAt(), request.reason(), request.force()));

    return ApiResponse.success(reload(drawId));
  }

  @Operation(summary = "Record a manual draw result (TENANT_ADMIN+)")
  @PostMapping("/{drawId}/manual-result")
  @PreAuthorize("hasPermission(null, 'draw_result.record_manual')")
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

    commandBus.execute(
        new RecordManualDrawResultCommand(
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
