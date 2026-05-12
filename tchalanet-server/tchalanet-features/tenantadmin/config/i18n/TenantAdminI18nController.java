package com.tchalanet.server.features.tenantadmin.config.i18n;

import com.tchalanet.server.common.types.id.I18nOverrideId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.AdminI18nRow;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.UpsertI18nOverrideRequest;
import com.tchalanet.server.features.tenantadmin.config.i18n.model.UpsertI18nOverrideResult;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config/i18n")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminI18nController {

    private final TenantAdminI18nService service;

    @GetMapping
    public ApiResponse<TchPage<AdminI18nRow>> search(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(value = "locale", required = false) String locale,
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "active", required = false) Boolean active,
        TchPageRequest pageRequest
    ) {
        return ApiResponse.success(service.search(ctx, locale, q, active, pageRequest));
    }

    @PutMapping
    public ApiResponse<UpsertI18nOverrideResult> upsert(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody UpsertI18nOverrideRequest req
    ) {
        return ApiResponse.success(service.upsert(ctx, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentContext TchRequestContext ctx, @PathVariable I18nOverrideId id) {
        service.delete(ctx, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/resolve")
    public ApiResponse<Map<String, String>> resolvePreview(
        @CurrentContext TchRequestContext ctx,
        @RequestParam("locale") String locale
    ) {
        return ApiResponse.success(service.resolvePreview(ctx, locale));
    }
}
