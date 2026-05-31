package com.tchalanet.server.catalog.settings.internal.web;

import com.tchalanet.server.catalog.settings.api.SettingsAdminCatalog;
import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public settings runtime read endpoint.
 *
 * <p>No authentication required. Always filters by PUBLIC_RUNTIME server-side.
 * Clients cannot request other exposure levels.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
@Tag(name = "Public • Settings", description = "Public runtime settings reads")
public class PublicSettingsRuntimeController {

    private final SettingsAdminCatalog catalog;

    @Operation(
        summary = "Load public runtime settings",
        description = "Returns active PUBLIC_RUNTIME settings. namespace is optional.")
    @GetMapping("/public/settings")
    public ApiResponse<List<SettingView>> getPublicSettings(
        @RequestParam(required = false) String namespace) {

        return ApiResponse.success(catalog.listActiveByExposure(SettingExposure.PUBLIC_RUNTIME, namespace));
    }
}
