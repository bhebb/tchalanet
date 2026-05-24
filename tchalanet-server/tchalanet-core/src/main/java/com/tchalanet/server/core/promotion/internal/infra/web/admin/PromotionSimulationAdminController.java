package com.tchalanet.server.core.promotion.internal.infra.web.admin;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionQuery;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.mapper.PromotionSimulationWebMapper;
import com.tchalanet.server.core.promotion.internal.infra.web.admin.request.SimulatePromotionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/promotions/simulations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
public class PromotionSimulationAdminController {

    private final QueryBus queryBus;
    private final PromotionSimulationWebMapper mapper;

    @PostMapping("/sale")
    @PreAuthorize("hasPermission(null, 'promotion.read')")
    public ApiResponse<PromotionDecision> simulateSale(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody SimulatePromotionRequest request
    ) {
        var evaluationContext = mapper.toEvaluationContext(
            ctx.effectiveTenantIdRequired(),
            request
        );

        var decision = queryBus.ask(new EvaluatePromotionQuery(evaluationContext));
        return ApiResponse.success(decision);
    }
}
