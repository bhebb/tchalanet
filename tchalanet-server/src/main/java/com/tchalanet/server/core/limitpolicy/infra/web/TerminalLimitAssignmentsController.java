package com.tchalanet.server.core.limitpolicy.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.limitpolicy.application.command.model.CreateLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsResult;
import com.tchalanet.server.core.limitpolicy.domain.model.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenant/terminals/{terminalId}/limit-assignments")
@RequiredArgsConstructor
public class TerminalLimitAssignmentsController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping
    public GetLimitAssignmentsResult getAssignments(@PathVariable UUID terminalId, @RequestParam UUID tenantId) {
        return queryBus.send(new GetLimitAssignmentsQuery(tenantId, TargetType.TERMINAL, terminalId));
    }

    @PostMapping
    public Object createAssignment(@PathVariable UUID terminalId, @RequestParam UUID tenantId, @RequestBody CreateLimitAssignmentRequest request) {
        var cmd = new CreateLimitAssignmentCommand(
                tenantId,
                request.limitDefinitionId(),
                TargetType.TERMINAL,
                terminalId,
                request.enabled(),
                request.startsAt(),
                request.endsAt()
        );
        return commandBus.send(cmd);
    }

    @DeleteMapping("/{assignmentId}")
    public void deleteAssignment(@PathVariable UUID terminalId, @PathVariable UUID assignmentId, @RequestParam UUID tenantId) {
        var cmd = new DeleteLimitAssignmentCommand(tenantId, assignmentId, TargetType.TERMINAL, terminalId);
        commandBus.send(cmd);
    }

    public record CreateLimitAssignmentRequest(UUID limitDefinitionId, boolean enabled, java.time.Instant startsAt, java.time.Instant endsAt) {}
}
