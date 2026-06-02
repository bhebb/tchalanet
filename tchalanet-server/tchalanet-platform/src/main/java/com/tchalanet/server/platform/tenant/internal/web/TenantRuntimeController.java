package com.tchalanet.server.platform.tenant.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.tenant.api.model.view.TenantRuntimeView;
import com.tchalanet.server.platform.tenant.internal.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tenant Runtime")
@RestController
@RequiredArgsConstructor
public class TenantRuntimeController {

    private final TenantConfigService tenants;

    @Operation(summary = "Public tenant runtime info — safe, no auth required")
    @GetMapping("/public/tenant/runtime")
    public ApiResponse<TenantRuntimeView> publicRuntime(
        @CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(tenants.getTenantRuntimeView(ctx.effectiveTenantCode()));
    }

    @Operation(summary = "Authenticated tenant runtime info")
    @GetMapping("/tenant/runtime")
    public ApiResponse<TenantRuntimeView> runtime(
        @CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(tenants.getTenantRuntimeView(ctx.effectiveTenantCode()));
    }
}
