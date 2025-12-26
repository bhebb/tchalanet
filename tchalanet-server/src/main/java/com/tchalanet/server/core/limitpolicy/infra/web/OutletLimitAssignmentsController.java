package com.tchalanet.server.core.limitpolicy.infra.web;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.core.limitpolicy.application.command.model.CreateLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitAssignmentsResult;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenant/outlets/{outletId}/limit-assignments")
@RequiredArgsConstructor
public class OutletLimitAssignmentsController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping
    public GetLimitAssignmentsResult getAssignments(@PathVariable OutletId outletId, @RequestParam TenantId tenantId) {
        return queryBus.send(new GetLimitAssignmentsQuery(tenantId, TargetType.OUTLET, outletId.uuid()));
    }

    @PostMapping
    public LimitAssignment createAssignment(@PathVariable OutletId outletId, @RequestParam TenantId tenantId, @RequestBody CreateLimitAssignmentRequest request) {
        var cmd = new CreateLimitAssignmentCommand(
            tenantId,
            request.limitDefinitionId(),
            TargetType.OUTLET,
            outletId.uuid(),
            request.enabled(),
            request.startsAt(),
            request.endsAt()
        );
        return commandBus.send(cmd);
    }

    @DeleteMapping("/{assignmentId}")
    public void deleteAssignment(@PathVariable OutletId outletId, @PathVariable UUID assignmentId, @RequestParam TenantId tenantId) {
        var cmd = new DeleteLimitAssignmentCommand(tenantId, assignmentId, TargetType.OUTLET, outletId.uuid());
        commandBus.send(cmd);
    }

    public record CreateLimitAssignmentRequest(UUID limitDefinitionId, boolean enabled, java.time.Instant startsAt,
                                               java.time.Instant endsAt) {
    }
}
