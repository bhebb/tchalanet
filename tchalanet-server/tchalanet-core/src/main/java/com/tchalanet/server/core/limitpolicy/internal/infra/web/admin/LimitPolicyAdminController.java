package com.tchalanet.server.core.limitpolicy.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.limitpolicy.api.command.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.api.command.DeleteLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.api.command.UpsertLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.api.command.UpsertLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.api.query.LimitRuleSpec;
import com.tchalanet.server.core.limitpolicy.api.query.ListAvailableLimitRulesQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.infra.web.admin.model.UpsertLimitAssignmentRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/policies/limits")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Limit Policy • Admin")
@Validated
public class LimitPolicyAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping("/assignments")
    @PreAuthorize("hasPermission('limitpolicy.read')")
    public ApiResponse<ListLimitAssignmentsView> listAssignments(
        @CurrentContext TchRequestContext context,
        @RequestParam("target") TargetType targetType,
        @RequestParam(value = "targetId", required = false) String targetId
    ) {
        var scope = toLimitScopeQueryRef(context, targetType, targetId);

        return ApiResponse.success(
            queryBus.ask(new ListLimitAssignmentsByScopeQuery(scope)));
    }

    @PutMapping("/assignments")
    @PreAuthorize("hasPermission('limitpolicy.admin')")
    public ApiResponse<UpsertLimitAssignmentResult> upsertAssignment(
        @CurrentContext TchRequestContext context,
        @Valid @RequestBody UpsertLimitAssignmentRequest req
    ) {
        var scope = toLimitScopeRef(
            context,
            req.targetType(),
            req.targetId());

        var cmd = new UpsertLimitAssignmentCommand(
            context.effectiveTenantIdRequired(),
            req.ruleKey(),
            scope,
            req.enabled(),
            req.onBreach(),
            req.params(),
            req.startsAt(),
            req.endsAt());

        return ApiResponse.success(commandBus.execute(cmd));
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasPermission('limitpolicy.admin')")
    public ApiResponse<DeleteLimitAssignmentResult> deleteAssignment(
        @PathVariable LimitAssignmentId id
    ) {
        return ApiResponse.success(
            commandBus.execute(new DeleteLimitAssignmentCommand(id)));
    }

    @GetMapping("/rules")
    @PreAuthorize("hasPermission('limitpolicy.read')")
    public ApiResponse<List<LimitRuleSpec>> listAvailableRules() {
        return ApiResponse.success(
            queryBus.ask(new ListAvailableLimitRulesQuery()));
    }

    private LimitScopeRef toLimitScopeRef(
        TchRequestContext context,
        TargetType targetType,
        String targetId
    ) {
        if (targetType == null) {
            throw new IllegalArgumentException("targetType is required");
        }

        return switch (targetType) {
            case TENANT -> LimitScopeRef.tenant(context.effectiveTenantIdRequired());

            case OUTLET -> {
                requireTargetId(targetType, targetId);
                yield LimitScopeRef.outlet(OutletId.parse(targetId));
            }

            case AGENT -> {
                requireTargetId(targetType, targetId);
                yield LimitScopeRef.agent(UserId.parse(targetId));
            }

            case DRAW_CHANNEL -> {
                requireTargetId(targetType, targetId);
                yield LimitScopeRef.drawChannel(DrawChannelId.parse(targetId));
            }

            default -> throw new IllegalArgumentException("Unsupported targetType: " + targetType);
        };
    }

    private LimitScopeQueryRef toLimitScopeQueryRef(
        TchRequestContext context,
        TargetType targetType,
        String targetId
    ) {
        if (targetType == null) {
            throw new IllegalArgumentException("targetType is required");
        }

        return switch (targetType) {
            case TENANT -> LimitScopeQueryRef.tenant(context.effectiveTenantIdRequired());

            case OUTLET -> {
                requireTargetId(targetType, targetId);
                yield LimitScopeQueryRef.outlet(OutletId.parse(targetId));
            }

            case AGENT -> {
                requireTargetId(targetType, targetId);
                yield LimitScopeQueryRef.agent(UserId.parse(targetId));
            }

            case DRAW_CHANNEL -> {
                requireTargetId(targetType, targetId);
                yield LimitScopeQueryRef.drawChannel(DrawChannelId.parse(targetId));
            }

            default -> throw new IllegalArgumentException("Unsupported targetType: " + targetType);
        };
    }

    private void requireTargetId(TargetType targetType, String targetId) {
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("targetId is required for " + targetType);
        }
    }
}
