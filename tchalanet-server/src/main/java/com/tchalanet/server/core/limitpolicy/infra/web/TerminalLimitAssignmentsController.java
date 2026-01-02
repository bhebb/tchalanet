package com.tchalanet.server.core.limitpolicy.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.limitpolicy.application.command.model.CreateLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsResult;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant/terminals/{terminalId}/limit-assignments")
@RequiredArgsConstructor
@Tag(name = "Tenant • Limits")
public class TerminalLimitAssignmentsController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @Operation(summary = "Get limit assignments for a terminal (tenant)")
  @GetMapping
  public GetLimitAssignmentsResult getAssignments(
      @PathVariable TerminalId terminalId, @RequestParam TenantId tenantId) {
    return queryBus.send(
        new GetLimitAssignmentsQuery(tenantId, TargetType.TERMINAL, terminalId.uuid()));
  }

  @Operation(summary = "Create limit assignment for a terminal (tenant)")
  @PostMapping
  public LimitAssignment createAssignment(
      @PathVariable TerminalId terminalId,
      @RequestParam TenantId tenantId,
      @RequestBody CreateLimitAssignmentRequest request) {
    var cmd =
        new CreateLimitAssignmentCommand(
            tenantId,
            request.limitDefinitionId(),
            TargetType.TERMINAL,
            terminalId.uuid(),
            request.enabled(),
            request.startsAt(),
            request.endsAt());
    return commandBus.send(cmd);
  }

  @Operation(summary = "Delete a limit assignment for a terminal (tenant)")
  @DeleteMapping("/{assignmentId}")
  public void deleteAssignment(
      @PathVariable TerminalId terminalId,
      @PathVariable UUID assignmentId,
      @RequestParam TenantId tenantId) {
    var cmd =
        new DeleteLimitAssignmentCommand(
            tenantId, assignmentId, TargetType.TERMINAL, terminalId.uuid());
    commandBus.send(cmd);
  }

  public record CreateLimitAssignmentRequest(
      UUID limitDefinitionId,
      boolean enabled,
      java.time.Instant startsAt,
      java.time.Instant endsAt) {}
}
