package com.tchalanet.server.platform.tenanttheme.internal.web;

import com.tchalanet.server.catalog.plan.api.PlanFeatureKeys;
import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.entitlement.api.RequiredFeature;
import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeAdminView;
import com.tchalanet.server.platform.tenanttheme.api.model.UpdateTenantThemeSettingsRequest;
import com.tchalanet.server.platform.tenanttheme.internal.service.TenantThemeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin • Theme")
@RestController
@RequestMapping("/admin/theme")
@PreAuthorize("hasPermission('theme.read')")
@RequiredArgsConstructor
public class TenantThemeAdminController {

    private final TenantThemeAdminService adminService;
    private final ThemeCatalog themeCatalog;

    @Operation(summary = "Get current tenant theme")
    @GetMapping
    public ApiResponse<TenantThemeAdminView> get(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(adminService.getAdminView(ctx.effectiveTenantIdRequired()));
    }

    @Operation(summary = "List available theme presets")
    @GetMapping("/presets")
    public ApiResponse<List<ThemePresetView>> listPresets() {
        return ApiResponse.success(themeCatalog.listActive());
    }

    @Operation(summary = "Apply a theme preset")
    @PostMapping("/preset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('theme.manage')")
    @RequiredFeature(PlanFeatureKeys.THEME_PRESET_SELECTION)
    public ApiResponse<Void> applyPreset(
        @Valid @RequestBody ApplyPresetRequest body,
        @CurrentContext TchRequestContext ctx) {
        adminService.applyPreset(new ApplyTenantThemeRequest(ctx.effectiveTenantIdRequired(), body.presetCode()));
        return ApiResponse.success(null);
    }

    @Operation(summary = "Update theme settings (defaultMode)")
    @PatchMapping("/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('theme.manage')")
    public ApiResponse<Void> updateSettings(
        @Valid @RequestBody UpdateSettingsRequest body,
        @CurrentContext TchRequestContext ctx) {
        adminService.updateSettings(new UpdateTenantThemeSettingsRequest(
            ctx.effectiveTenantIdRequired(), body.defaultMode()));
        return ApiResponse.success(null);
    }

    @Operation(summary = "Deactivate tenant theme (reset to default)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasPermission('theme.manage')")
    public ApiResponse<Void> deactivate(@CurrentContext TchRequestContext ctx) {
        adminService.deactivate(new DeactivateTenantThemeRequest(ctx.effectiveTenantIdRequired()));
        return ApiResponse.success(null);
    }

    public record ApplyPresetRequest(@NotBlank String presetCode) {}

    public record UpdateSettingsRequest(String defaultMode) {}
}
