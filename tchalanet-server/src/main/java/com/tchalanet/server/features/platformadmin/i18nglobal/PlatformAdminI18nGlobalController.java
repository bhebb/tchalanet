package com.tchalanet.server.features.platformadmin.i18nglobal;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/i18n-global")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformAdminI18nGlobalController {

    private final I18nOverridesCatalog catalog;

    // -------------------------------------------------
    // Overview
    // -------------------------------------------------

    @GetMapping("/overview")
    public ApiResponse<I18nGlobalOverviewView> overview() {
        var stats = catalog.keyStats();
        return ApiResponse.success(
            new I18nGlobalOverviewView(
                Instant.now(),
                new I18nGlobalOverviewView.Summary(
                    stats.totalKeys(),
                    stats.totalLocales(),
                    stats.totalOverrides()
                )));
    }

    // -------------------------------------------------
    // Resolve (SAFE)
    // -------------------------------------------------

    /**
     * Resolve i18n bundle.
     *
     * Behavior:
     * - no tenantId -> GLOBAL only
     * - tenantId provided -> GLOBAL + TENANT overrides for that tenant
     */
    @GetMapping("/resolve")
    public ApiResponse<Map<String, String>> resolve(
        @RequestParam String locale,
        @RequestParam(required = false) TenantId tenantId) {

        Map<String, String> resolved =
            tenantId == null
                ? catalog.resolveLocale(locale) // GLOBAL only (platform scope)
                : catalog.resolveLocaleForTenant(locale, tenantId);

        return ApiResponse.success(resolved);
    }

    // -------------------------------------------------
    // Search (cross-tenant, paginated)
    // -------------------------------------------------

    @PostMapping("/search")
    public ApiResponse<TchPage<I18nOverrideView>> search(
        @RequestBody SearchI18nOverridesCriteria criteria,
        TchPageRequest pageRequest) {

        return ApiResponse.success(catalog.search(criteria, pageRequest));
    }

    // -------------------------------------------------
    // Views
    // -------------------------------------------------

    public record I18nGlobalOverviewView(
        Instant generatedAt,
        Summary summary) {

        public record Summary(
            long totalKeys,
            long totalLocales,
            long totalOverrides) {}
    }
}
