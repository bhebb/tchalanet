package com.tchalanet.server.features.tenantadmin.config.settings;

import com.tchalanet.server.common.types.id.SettingId;
import com.tchalanet.server.common.apiresponse.ApiResponse;
import com.tchalanet.server.common.paging.TchPageRequest;
import com.tchalanet.server.features.tenantadmin.config.settings.model.AdminSettingRow;
import com.tchalanet.server.features.tenantadmin.config.settings.model.UpsertTenantSettingRequest;
import com.tchalanet.server.features.tenantadmin.config.settings.model.UpsertTenantSettingResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/config/settings")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminSettingsController {

    private final TenantAdminSettingsService service;

    @GetMapping
    public ApiResponse<List<AdminSettingRow>> search(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(value = "namespace", required = false) String namespace,
        @RequestParam(value = "settingKey", required = false) String settingKey,
        @RequestParam(value = "active", required = false) Boolean active,
        TchPageRequest pageRequest
    ) {
        return ApiResponse.success(service.search(ctx, namespace, settingKey, active, pageRequest));
    }

    @PutMapping
    public ApiResponse<UpsertTenantSettingResult> upsert(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody UpsertTenantSettingRequest req
    ) {
        return ApiResponse.success(service.upsert(ctx, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentContext TchRequestContext ctx, @PathVariable SettingId id) {
        service.delete(ctx, id);
        return ApiResponse.success(null);
    }
}
