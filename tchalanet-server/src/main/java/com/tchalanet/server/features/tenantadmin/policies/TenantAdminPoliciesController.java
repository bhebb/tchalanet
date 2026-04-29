package com.tchalanet.server.features.tenantadmin.policies;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyOverviewQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitAssignmentsByTargetQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitDefinitionsQuery;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;
import com.tchalanet.server.features.tenantadmin.policies.PoliciesOverviewView;
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
        var defs = queryBus.send(new ListLimitDefinitionsQuery());
        int defsCount = defs.items() == null ? 0 : defs.items().size();

        var tenantAssignments = queryBus.send(new ListLimitAssignmentsByTargetQuery(LimitTarget.tenant()));
        int tenantAsgCount = tenantAssignments.items() == null ? 0 : tenantAssignments.items().size();

        var autonomy = queryBus.send(new GetAutonomyOverviewQuery(AutonomyTargetType.TENANT, ctx.tenantIdSafe().value()));
        boolean autonomyConfigured = autonomy.rule() != null;
        String autonomyLevel = autonomy.rule() == null ? null : autonomy.rule().level().name();

        return ApiResponse.success(new PoliciesOverviewView(defsCount, tenantAsgCount, autonomyConfigured, autonomyLevel));
    }
}
