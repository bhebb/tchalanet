package com.tchalanet.server.core.autonomy.infra.web.admin;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.autonomy.application.command.handler.UpsertAutonomyPolicyRuleCommandHandler;
import com.tchalanet.server.core.autonomy.application.command.model.UpsertAutonomyPolicyRuleCommand;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyOverviewView;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyOverviewQuery;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;
import com.tchalanet.server.core.autonomy.infra.web.admin.model.UpsertAutonomyRuleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/policies/autonomy")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AutonomyAdminController {

    private final QueryBus queryBus;
    private final UpsertAutonomyPolicyRuleCommandHandler autonomyHandler;

    @GetMapping
    public ApiResponse<AutonomyOverviewView> getOverview(
        @RequestParam("targetType") AutonomyTargetType targetType,
        @RequestParam(value = "targetId", required = false) UUID targetId) {
        return ApiResponse.success(queryBus.ask(new GetAutonomyOverviewQuery(targetType, targetId)));
    }

    @PutMapping
    public ApiResponse<AutonomyOverviewView> upsert(@CurrentContext TchRequestContext ctx, @Valid @RequestBody UpsertAutonomyRuleRequest req) {
        var targetId = req.targetId() == null ? null : AutonomyTargetId.of(req.targetId());
        var cmd = new UpsertAutonomyPolicyRuleCommand(
            req.targetType(), targetId, req.level(), req.requireApprovalOnBlock(),
            req.approvalRole(), req.enabled(), req.startsAt(), req.endsAt(), req.expectedVersion());
        autonomyHandler.handle(cmd);

        UUID effectiveTargetUuid = req.targetId();
        if (req.targetType() == AutonomyTargetType.TENANT && effectiveTargetUuid == null) {
            effectiveTargetUuid = ctx.tenantIdSafe().value();
        }
        return ApiResponse.success(queryBus.ask(new GetAutonomyOverviewQuery(req.targetType(), effectiveTargetUuid)));
    }
}
