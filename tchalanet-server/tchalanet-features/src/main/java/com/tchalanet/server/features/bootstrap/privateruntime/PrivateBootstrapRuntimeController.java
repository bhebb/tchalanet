package com.tchalanet.server.features.bootstrap.privateruntime;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.bootstrap.PrivateRuntimeStateResponse;
import com.tchalanet.server.features.bootstrap.RuntimeBootstrapResponse;
import com.tchalanet.server.features.bootstrap.RuntimeBootstrapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Runtime")
public class PrivateBootstrapRuntimeController {

    private final RuntimeBootstrapService service;

    @GetMapping({"/runtime/private", "/tenant/runtime/bootstrap", "/tenant/me/bootstrap"})
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN') or hasAuthority('ACTOR_SELLER_TERMINAL')")
    @Operation(summary = "Private runtime bootstrap — resolves user, authorization and runtime context")
    public ApiResponse<RuntimeBootstrapResponse> privateBootstrap(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(service.privateBootstrap(ctx));
    }

    @GetMapping("/tenant/runtime/state")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN') or hasAuthority('ACTOR_SELLER_TERMINAL')")
    @Operation(summary = "Lightweight private runtime state — readiness, notifications, blocking and version hints")
    public ApiResponse<PrivateRuntimeStateResponse> privateState(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(service.privateState(ctx));
    }
}
