package com.tchalanet.server.core.subscription.internal.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.subscription.api.command.ApplyTenantPlanCommand;
import com.tchalanet.server.core.subscription.api.command.ApplyTenantPlanResult;
import com.tchalanet.server.core.subscription.api.command.ChangePlanCommand;
import com.tchalanet.server.core.subscription.api.command.ChangePlanResult;
import com.tchalanet.server.core.subscription.api.query.ResolveTenantSubscriptionQuery;
import com.tchalanet.server.core.subscription.api.query.SubscriptionView;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformSubscriptionController {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping("/{tenantId}")
    public ApiResponse<SubscriptionView> resolve(@PathVariable TenantId tenantId) {
        var view = queryBus.ask(new ResolveTenantSubscriptionQuery(tenantId));
        return ApiResponse.success(view);
    }

    @PostMapping("/{tenantId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    @AuditLog(action = AuditAction.CREATE, entity = AuditEntityType.SUBSCRIPTION,
              idExpression = "#result.subscriptionId",
              detailsExpression = "{ 'planCode': #req.planCode(), 'tenantId': #tenantId }")
    public ApiResponse<ApplyTenantPlanResult> apply(
            @PathVariable TenantId tenantId,
            @Valid @RequestBody ApplyPlanRequest req) {
        var cmd = new ApplyTenantPlanCommand(tenantId, req.planCode(), req.effectiveAt(), null);
        return ApiResponse.created(commandBus.execute(cmd));
    }

    @PostMapping("/{tenantId}/change")
    @AuditLog(action = AuditAction.UPDATE, entity = AuditEntityType.SUBSCRIPTION,
              idExpression = "#result.subscriptionId",
              detailsExpression = "{ 'newPlanCode': #req.newPlanCode(), 'tenantId': #tenantId }")
    public ApiResponse<ChangePlanResult> change(
            @PathVariable TenantId tenantId,
            @Valid @RequestBody ChangePlanRequest req) {
        var cmd = new ChangePlanCommand(tenantId, req.newPlanCode(), req.effectiveAt(), null);
        return ApiResponse.success(commandBus.execute(cmd));
    }
}
