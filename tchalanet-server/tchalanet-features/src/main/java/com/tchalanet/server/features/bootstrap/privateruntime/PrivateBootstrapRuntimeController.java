package com.tchalanet.server.features.bootstrap.privateruntime;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.bootstrap.app.RuntimeBootstrapService;
import com.tchalanet.server.features.bootstrap.model.PrivateRuntimeStateResponse;
import com.tchalanet.server.features.bootstrap.model.PublicBootstrapResponse;
import com.tchalanet.server.features.bootstrap.model.RuntimeBootstrapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single runtime controller. Public and private routes both bootstrap through {@code /runtime/*}.
 * Authorization is declared per-method so the public endpoint can stay unauthenticated.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Runtime")
public class PrivateBootstrapRuntimeController {

    private final RuntimeBootstrapService service;

    @GetMapping({"/runtime/private", "/tenant/runtime/bootstrap"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'CASHIER', 'OPERATOR')")
    @Operation(summary = "Private runtime bootstrap — resolves Tchalanet-owned user, authorization and runtime context")
    public ApiResponse<RuntimeBootstrapResponse> privateBootstrap(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(service.privateBootstrap(ctx));
    }

    @GetMapping("/tenant/runtime/state")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'CASHIER', 'OPERATOR')")
    @Operation(summary = "Lightweight private runtime state — readiness, notifications, blocking and version hints")
    public ApiResponse<PrivateRuntimeStateResponse> privateState(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(service.privateState(ctx));
    }

    @GetMapping("/public/runtime/bootstrap")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Public runtime bootstrap (no auth) — public settings, theme, i18n, navigation, readiness and pageModelRef")
    public ApiResponse<PublicBootstrapResponse> publicBootstrap(
        @RequestParam(required = false) String locale) {
        return ApiResponse.success(service.publicBootstrap(locale));
    }
}
