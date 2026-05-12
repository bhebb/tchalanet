package com.tchalanet.server.features.tenantadmin.policies;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyOverviewQuery;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetId;
import com.tchalanet.server.core.limitpolicy.application.query.model.assignment.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/policies")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminPoliciesController {

    private final QueryBus queryBus;

    @GetMapping("/overview")
    public ApiResponse<PoliciesOverviewView> overview(@CurrentContext TchRequestContext ctx) {

        var tenantAsgCount = queryBus.ask(new ListLimitAssignmentsByScopeQuery(LimitScopeRef.tenant(ctx.tenantId())));

        var autonomy = queryBus.ask(new GetAutonomyOverviewQuery(AutonomyTargetType.TENANT, AutonomyTargetId.of(ctx.tenantUuid())));
        boolean autonomyConfigured = autonomy.rule() != null;
        String autonomyLevel = autonomy.rule() == null ? null : autonomy.rule().level().name();

        return ApiResponse.success(new PoliciesOverviewView(tenantAsgCount.items().size(), autonomyConfigured, autonomyLevel));
    }
}
