package com.tchalanet.server.core.autonomy.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.autonomy.api.command.DeleteAutonomyRuleCommand;
import com.tchalanet.server.core.autonomy.api.command.UpsertAutonomyRuleCommand;
import com.tchalanet.server.core.autonomy.api.query.AutonomyOverviewView;
import com.tchalanet.server.core.autonomy.api.query.GetAutonomyOverviewQuery;
import com.tchalanet.server.core.autonomy.internal.domain.model.AutonomyTargetId;
import com.tchalanet.server.core.autonomy.internal.infra.web.admin.mapper.AutonomyAdminWebMapper;
import com.tchalanet.server.core.autonomy.internal.infra.web.admin.model.AutonomyOverviewResponse;
import com.tchalanet.server.core.autonomy.internal.infra.web.admin.model.UpsertAutonomyRuleRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/policies/autonomy")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Autonomy • Admin")
@Validated
public class AutonomyAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final AutonomyAdminWebMapper mapper;

    @GetMapping
    @PreAuthorize("hasPermission('autonomy.read')")
    public ApiResponse<AutonomyOverviewResponse> getOverview(
        @RequestParam("targetType") AutonomyTargetType targetType,
        @RequestParam(value = "targetId", required = false) AutonomyTargetId targetId
    ) {
        var view = queryBus.ask(new GetAutonomyOverviewQuery(targetType, targetId));
        return ApiResponse.success(mapper.toResponse(view));
    }

    @PutMapping
    @PreAuthorize("hasPermission('autonomy.admin')")
    public ApiResponse<AutonomyOverviewResponse> upsert(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody UpsertAutonomyRuleRequest req
    ) {
        var cmd = new UpsertAutonomyRuleCommand(
            ctx.effectiveTenantIdRequired(),
            req.targetType(),
            req.targetId(),
            req.level(),
            req.requireApprovalOnBlock(),
            req.approvalRole(),
            req.enabled(),
            req.startsAt(),
            req.endsAt());

        commandBus.execute(cmd);

        var view = queryBus.ask(new GetAutonomyOverviewQuery(req.targetType(), req.targetId()));
        return ApiResponse.success(mapper.toResponse(view));
    }

    @DeleteMapping
    @PreAuthorize("hasPermission('autonomy.admin')")
    public ApiResponse<Void> delete(
        @CurrentContext TchRequestContext ctx,
        @RequestParam("targetType") AutonomyTargetType targetType,
        @RequestParam(value = "targetId", required = false) AutonomyTargetId targetId
    ) {
        commandBus.execute(new DeleteAutonomyRuleCommand(
            ctx.effectiveTenantIdRequired(),
            targetType,
            targetId));

        return ApiResponse.success(null);
    }
}
