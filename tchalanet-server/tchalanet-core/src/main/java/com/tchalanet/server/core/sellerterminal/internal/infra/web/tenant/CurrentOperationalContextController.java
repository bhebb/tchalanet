package com.tchalanet.server.core.sellerterminal.internal.infra.web.tenant;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.sellerterminal.api.query.CurrentOperationalContextView;
import com.tchalanet.server.core.sellerterminal.api.query.GetCurrentOperationalContextQuery;
import com.tchalanet.server.platform.accesscontrol.api.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    "/tenant/seller-terminal/operational-context",
    "/tenant/me/operational-context"
})
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL')")
@Tag(name = "Terminal • Operational Context")
@RequiredArgsConstructor
public class CurrentOperationalContextController {

    private final QueryBus queryBus;

    @GetMapping
    @Operation(summary = "Read the operational context attached to the current request")
    @RequiresPermission("seller_terminal.operational_context.read")
    public ApiResponse<CurrentOperationalContextView> current(@CurrentContext TchRequestContext ctx) {
        var operationalContext = ctx.operationalContext();
        return ApiResponse.success(queryBus.ask(new GetCurrentOperationalContextQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.sellerTerminalIdRequired(),
            operationalContext != null ? operationalContext.source() : null,
            operationalContext != null ? operationalContext.trust() : null,
            operationalContext != null && operationalContext.trustedForSensitiveOperation())));
    }
}
