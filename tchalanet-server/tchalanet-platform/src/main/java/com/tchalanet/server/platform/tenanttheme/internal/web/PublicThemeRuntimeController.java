package com.tchalanet.server.platform.tenanttheme.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.tenanttheme.api.model.ThemeRuntimeView;
import com.tchalanet.server.platform.tenanttheme.internal.service.TenantThemeRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public • Theme Runtime")
@RestController
@RequiredArgsConstructor
public class PublicThemeRuntimeController {

    private final TenantThemeRuntimeService runtimeService;

    @Operation(summary = "Public theme runtime — safe, no auth required")
    @GetMapping("/public/theme/runtime")
    public ApiResponse<ThemeRuntimeView> publicRuntime(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(required = false) String mode) {
        return ApiResponse.success(
            runtimeService.getRuntime(ctx.effectiveTenantIdRequired(), mode));
    }

    @Operation(summary = "Authenticated theme runtime (with user mode preference)")
    @GetMapping("/tenant/theme/runtime")
    public ApiResponse<ThemeRuntimeView> runtime(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(required = false) String mode) {
        return ApiResponse.success(
            runtimeService.getRuntime(ctx.effectiveTenantIdRequired(), mode));
    }
}
