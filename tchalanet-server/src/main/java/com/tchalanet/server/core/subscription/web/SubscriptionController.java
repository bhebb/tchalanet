package com.tchalanet.server.core.subscription.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.subscription.application.command.model.*;
import com.tchalanet.server.core.subscription.application.query.model.ResolveTenantSubscriptionQuery;
import com.tchalanet.server.core.subscription.application.query.model.SubscriptionView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/tenant/subscription")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
public class SubscriptionController {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @PostMapping("/apply")
    @AuditLog(action = AuditAction.CREATE, entity = AuditEntityType.SUBSCRIPTION, idExpression = "#result.subscriptionId", detailsExpression = "{ 'planCode': #cmd.planCode(), 'tenantId': #cmd.tenantId() }")
    public ResponseEntity<ApiResponse<ApplyTenantPlanResult>> apply(@RequestBody ApplyTenantPlanCommand cmd) {
        var result = commandBus.send(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_APPLIED", "Abonnement appliqué", "subscription", NoticeSeverity.INFO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result));
    }

    @PostMapping("/cancel")
    @AuditLog(action = AuditAction.DELETE, entity = AuditEntityType.SUBSCRIPTION, idExpression = "#result.subscriptionId", detailsExpression = "{ 'tenantId': #cmd.tenantId() }")
    public ResponseEntity<ApiResponse<CancelSubscriptionResult>> cancel(@RequestBody CancelSubscriptionCommand cmd) {
        var result = commandBus.send(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_CANCELED", "Abonnement annulé", "subscription", NoticeSeverity.INFO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/change")
    @AuditLog(action = AuditAction.UPDATE, entity = AuditEntityType.SUBSCRIPTION, idExpression = "#result.subscriptionId", detailsExpression = "{ 'oldPlanCode': #result.oldPlanCode, 'newPlanCode': #result.newPlanCode, 'tenantId': #cmd.tenantId() }")
    public ResponseEntity<ApiResponse<ChangePlanResult>> change(@RequestBody ChangePlanCommand cmd) {
        var result = commandBus.send(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_PLAN_CHANGED", "Changement de plan effectué", "subscription", NoticeSeverity.INFO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/renew")
    @AuditLog(action = AuditAction.UPDATE, entity = AuditEntityType.SUBSCRIPTION, idExpression = "#result.subscriptionId", detailsExpression = "{ 'newEndsAt': #result.newEndsAt, 'tenantId': #cmd.tenantId() }")
    public ResponseEntity<ApiResponse<RenewSubscriptionResult>> renew(@RequestBody RenewSubscriptionCommand cmd) {
        var result = commandBus.send(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_RENEWED", "Abonnement renouvelé", "subscription", NoticeSeverity.INFO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/resume")
    @AuditLog(action = AuditAction.RESTORE, entity = AuditEntityType.SUBSCRIPTION, idExpression = "#result.subscriptionId", detailsExpression = "{ 'tenantId': #cmd.tenantId() }")
    public ResponseEntity<ApiResponse<ResumeSubscriptionResult>> resume(@RequestBody ResumeSubscriptionCommand cmd) {
        var result = commandBus.send(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_RESUMED", "Abonnement réactivé", "subscription", NoticeSeverity.INFO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/suspend")
    @AuditLog(action = AuditAction.UPDATE, entity = AuditEntityType.SUBSCRIPTION, idExpression = "#result.subscriptionId", detailsExpression = "{ 'tenantId': #cmd.tenantId() }")
    public ResponseEntity<ApiResponse<SuspendSubscriptionResult>> suspend(@RequestBody SuspendSubscriptionCommand cmd) {
        var result = commandBus.send(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_SUSPENDED", "Abonnement suspendu", "subscription", NoticeSeverity.INFO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/resolve/{tenantId}")
    public ResponseEntity<ApiResponse<SubscriptionView>> resolve(@PathVariable("tenantId") TenantId tenantId) {
        var view = queryBus.send(new ResolveTenantSubscriptionQuery(tenantId));
        return ResponseEntity.ok(ApiResponse.success(view));
    }

}
