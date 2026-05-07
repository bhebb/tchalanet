package com.tchalanet.server.core.limitpolicy.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.limitpolicy.application.command.model.*;
import com.tchalanet.server.core.limitpolicy.application.query.model.*;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import com.tchalanet.server.core.limitpolicy.infra.web.admin.model.UpsertLimitAssignmentRequest;
import com.tchalanet.server.core.limitpolicy.infra.web.admin.model.UpsertLimitDefinitionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/policies")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class LimitPolicyAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping("/definitions")
    public ApiResponse<ListLimitDefinitionsView> listDefinitions() {
        return ApiResponse.success(queryBus.ask(new ListLimitDefinitionsQuery()));
    }

    @PutMapping("/definitions")
    public ApiResponse<UpsertLimitDefinitionResult> upsertDefinition(@Valid @RequestBody UpsertLimitDefinitionRequest req) {
        var cmd = new UpsertLimitDefinitionCommand(req.ruleKey(), req.enabled(), req.onBreach(), req.params(), req.appliesTo());
        return ApiResponse.success(commandBus.execute(cmd));
    }

    @DeleteMapping("/definitions/{id}")
    public ApiResponse<DeleteLimitDefinitionResult> deleteDefinition(@PathVariable LimitDefinitionId id) {
        return ApiResponse.success(commandBus.execute(new DeleteLimitDefinitionCommand(id)));
    }

    @GetMapping("/assignments")
    public ApiResponse<ListLimitAssignmentsView> listAssignments(
        @RequestParam("target") TargetType targetType,
        @RequestParam(value = "targetId", required = false) UUID targetId) {
        LimitTarget target = switch (targetType) {
            case TENANT -> LimitTarget.tenant();
            case OUTLET -> LimitTarget.outlet(OutletId.of(targetId));
            case TERMINAL -> LimitTarget.terminal(TerminalId.of(targetId));
            case AGENT -> LimitTarget.agent(AgentId.of(targetId));
            default -> throw new IllegalArgumentException("Unsupported targetType: " + targetType);
        };
        return ApiResponse.success(queryBus.ask(new ListLimitAssignmentsByTargetQuery(target)));
    }

    @PutMapping("/assignments")
    public ApiResponse<UpsertLimitAssignmentResult> upsertAssignment(@Valid @RequestBody UpsertLimitAssignmentRequest req) {
        var cmd = new UpsertLimitAssignmentCommand(req.limitDefinitionId(), req.target(), req.enabled(), req.startsAt(), req.endsAt(), req.paramsOverride(), req.appliesToOverride());
        return ApiResponse.success(commandBus.execute(cmd));
    }

    @DeleteMapping("/assignments/{id}")
    public ApiResponse<DeleteLimitAssignmentResult> deleteAssignment(@PathVariable LimitAssignmentId id) {
        return ApiResponse.success(commandBus.execute(new DeleteLimitAssignmentCommand(id)));
    }
}
