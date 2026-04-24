package com.tchalanet.server.features.pagemodel;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PageModelController {

    private final PageModelOrchestrator orchestrator;
    private final PageModelTypeResolver typeResolver;

    @GetMapping("/public/pagemodel/{logicalId}")
    public ApiResponse<PageModelResponse> publicResolve(@PathVariable String logicalId) {
        return ApiResponse.success(orchestrator.resolvePublic(logicalId));
    }

    @GetMapping("/tenant/pagemodel/{logicalId}")
    public ApiResponse<PageModelResponse> tenantResolve(
        @PathVariable String logicalId, @CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(orchestrator.resolveTenant(logicalId, ctx));
    }

    @GetMapping("/tenant/pagemodel/dashboard")
    public ApiResponse<PageModelResponse> tenantDashboard(@CurrentContext TchRequestContext ctx) {
        String logicalId = typeResolver.forDashboard(ctx.currentRole()).logicalId();
        return ApiResponse.success(orchestrator.resolveTenant(logicalId, ctx));
    }

    @GetMapping("/platform/pagemodel/{logicalId}")
    public ApiResponse<PageModelResponse> platformResolve(
        @PathVariable String logicalId, @CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(orchestrator.resolvePlatform(logicalId, ctx));
    }

    @GetMapping("/platform/pagemodel/dashboard")
    public ApiResponse<PageModelResponse> platformDashboard(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(orchestrator.resolvePlatform("private.dashboard.superadmin", ctx));
    }
}
