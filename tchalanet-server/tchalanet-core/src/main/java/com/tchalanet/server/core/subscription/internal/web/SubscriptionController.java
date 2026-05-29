package com.tchalanet.server.core.subscription.internal.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.subscription.api.command.*;
import com.tchalanet.server.core.subscription.api.query.ResolveTenantSubscriptionQuery;
import com.tchalanet.server.core.subscription.api.query.SubscriptionView;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/tenant/subscription")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
public class SubscriptionController {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping
    public ApiResponse<SubscriptionView> getMySubscription(@CurrentContext TchRequestContext ctx) {
        var view = queryBus.ask(new ResolveTenantSubscriptionQuery(ctx.effectiveTenantIdRequired()));
        return ApiResponse.success(view);
    }

    @PostMapping("/cancel")
    @AuditLog(action = AuditAction.DELETE, entity = AuditEntityType.SUBSCRIPTION,
              idExpression = "#result.subscriptionId",
              detailsExpression = "{ 'tenantId': #ctx.effectiveTenantIdRequired().value() }")
    public ApiResponse<CancelSubscriptionResult> cancel(
            @CurrentContext TchRequestContext ctx,
            @RequestBody(required = false) CancelRequest req) {
        var cmd = new CancelSubscriptionCommand(ctx.effectiveTenantIdRequired(), req != null ? req.reason() : null, null);
        var result = commandBus.execute(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_CANCELED", "Abonnement annulé", "subscription", NoticeSeverity.INFO);
        return ApiResponse.success(result);
    }

    @PostMapping("/renew")
    @AuditLog(action = AuditAction.UPDATE, entity = AuditEntityType.SUBSCRIPTION,
              idExpression = "#result.subscriptionId",
              detailsExpression = "{ 'newEndsAt': #result.newEndsAt, 'tenantId': #ctx.effectiveTenantIdRequired().value() }")
    public ApiResponse<RenewSubscriptionResult> renew(
            @CurrentContext TchRequestContext ctx,
            @Valid @RequestBody RenewRequest req) {
        var cmd = new RenewSubscriptionCommand(ctx.effectiveTenantIdRequired(), req.newEndsAt(), null);
        var result = commandBus.execute(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_RENEWED", "Abonnement renouvelé", "subscription", NoticeSeverity.INFO);
        return ApiResponse.success(result);
    }

    @PostMapping("/resume")
    @AuditLog(action = AuditAction.RESTORE, entity = AuditEntityType.SUBSCRIPTION,
              idExpression = "#result.subscriptionId",
              detailsExpression = "{ 'tenantId': #ctx.effectiveTenantIdRequired().value() }")
    public ApiResponse<ResumeSubscriptionResult> resume(@CurrentContext TchRequestContext ctx) {
        var cmd = new ResumeSubscriptionCommand(ctx.effectiveTenantIdRequired(), null);
        var result = commandBus.execute(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_RESUMED", "Abonnement réactivé", "subscription", NoticeSeverity.INFO);
        return ApiResponse.success(result);
    }

    @PostMapping("/suspend")
    @AuditLog(action = AuditAction.UPDATE, entity = AuditEntityType.SUBSCRIPTION,
              idExpression = "#result.subscriptionId",
              detailsExpression = "{ 'tenantId': #ctx.effectiveTenantIdRequired().value() }")
    public ApiResponse<SuspendSubscriptionResult> suspend(@CurrentContext TchRequestContext ctx) {
        var cmd = new SuspendSubscriptionCommand(ctx.effectiveTenantIdRequired(), null);
        var result = commandBus.execute(cmd);
        ApiResponseContext.get().addNotice("SUBSCRIPTION_SUSPENDED", "Abonnement suspendu", "subscription", NoticeSeverity.INFO);
        return ApiResponse.success(result);
    }

    public record CancelRequest(String reason) {}
    public record RenewRequest(@NotNull Instant newEndsAt) {}
}
