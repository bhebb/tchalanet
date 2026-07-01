package com.tchalanet.server.features.bootstrap.privateruntime;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.bootstrap.privateruntime.app.RuntimeBootstrapService;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateRuntimeStateResponse;
import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimeBootstrapResponse;
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
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Private runtime bootstrap — resolves user, authorization and runtime context")
    public ApiResponse<RuntimeBootstrapResponse> privateBootstrap(@CurrentContext TchRequestContext ctx) {
        var api = ApiResponse.success(service.privateBootstrap(ctx));
        return api;
    }

    @GetMapping("/tenant/runtime/state")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lightweight private runtime state — readiness, notifications, blocking and version hints")
    public ApiResponse<PrivateRuntimeStateResponse> privateState(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(service.privateState(ctx));
    }
}
