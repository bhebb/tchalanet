package com.tchalanet.server.features.tenantadmin.policies;

import com.tchalanet.server.common.context.web.CurrentContext;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.autonomy.api.AutonomyTargetType;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.autonomy.api.query.GetAutonomyOverviewQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/policies")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminPoliciesController {

    private final QueryBus queryBus;

    @GetMapping("/overview")
    public ApiResponse<PoliciesOverviewView> overview(@CurrentContext TchRequestContext ctx) {

        var tenantAsgCount = queryBus.ask(new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.tenant(ctx.tenantId())));

        var autonomy = queryBus.ask(new GetAutonomyOverviewQuery(AutonomyTargetType.TENANT, ctx.tenantUuid()));
        boolean autonomyConfigured = autonomy.rule() != null;
        String autonomyLevel = autonomy.rule() == null ? null : autonomy.rule().level().name();

        return ApiResponse.success(new PoliciesOverviewView(tenantAsgCount.items().size(), autonomyConfigured, autonomyLevel));
    }
}
