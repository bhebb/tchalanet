package com.tchalanet.server.features.bootstrap.publicruntime;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.bootstrap.publicruntime.model.PublicBootstrapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single runtime controller. Public and private routes both bootstrap through {@code /runtime/*}.
 * Authorization is declared per-method so the public endpoint can stay unauthenticated.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Runtime")
@RequestMapping("/public/runtime")
public class PublicBootstrapRuntimeController {

    private final PublicRuntimeBootstrapService service;

    @GetMapping("/bootstrap")
    @Operation(summary = "Public runtime bootstrap (no auth) — public settings, theme, i18n, navigation, readiness and pageModelRef")
    public ApiResponse<PublicBootstrapResponse> publicBootstrap(
        @RequestParam(required = false) String locale) {
        return ApiResponse.success(service.publicBootstrap(locale));
    }
}
