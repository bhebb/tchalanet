package com.tchalanet.server.features.tenantadmin.setup;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantDrawSalesMatrixView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin • Setup")
@RestController
@RequestMapping("/admin/setup")
@PreAuthorize("hasPermission('tenantgame.read')")
@RequiredArgsConstructor
public class AdminSetupController {

    private final TenantGamesPricingService gamesPricingService;
    private final TenantDrawSalesMatrixService drawSalesMatrixService;

    @Operation(summary = "Games & pricing configuration card")
    @GetMapping("/games-pricing")
    public ApiResponse<TenantGamesPricingView> gamesPricing(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(gamesPricingService.get(ctx.tenantId()));
    }

    @Operation(summary = "Draw sales matrix — channels × games readiness card")
    @GetMapping("/draw-sales-matrix")
    public ApiResponse<TenantDrawSalesMatrixView> drawSalesMatrix(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(drawSalesMatrixService.get(ctx.tenantId()));
    }
}
