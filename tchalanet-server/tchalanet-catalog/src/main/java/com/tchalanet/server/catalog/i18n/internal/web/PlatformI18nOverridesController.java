package com.tchalanet.server.catalog.i18n.internal.web;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.catalog.i18n.internal.web.model.CreateI18nOverrideRequest;
import com.tchalanet.server.catalog.i18n.internal.web.model.UpdateI18nOverrideRequest;
import com.tchalanet.server.catalog.i18n.internal.write.I18nOverridesAdminService;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.I18nOverrideId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Platform I18n Overrides Controller
 *
 * <p>Provides CRUD operations for platform administrators to manage tenant-specific i18n
 * translation overrides.
 *
 * <p>Security: PLATFORM_ADMIN role required (configured in SecurityConfig).
 *
 * <p>This controller delegates all logic to {@link I18nOverridesAdminService} and returns {@link
 * ApiResponse} wrappers.
 */
@PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SUPER_ADMIN')")
@RestController
@RequestMapping("/platform/i18n-overrides")
@RequiredArgsConstructor
@Tag(
    name = "Platform • I18n Overrides",
    description = "Platform admin CRUD for tenant-specific i18n translation overrides")
public class PlatformI18nOverridesController {

    private final I18nOverridesAdminService adminService;
    private final I18nOverridesCatalog i18nOverridesCatalog;

    @Operation(
        summary = "Search i18n overrides (paginated)",
        description = "Search i18n overrides with filters and pagination")
    @GetMapping
    public ApiResponse<TchPage<I18nOverrideView>> search(
        @RequestParam(required = false) String level,
        @RequestParam(required = false) String locale,
        @RequestParam(required = false) String i18nKeyContains,
        @RequestParam(required = false) Boolean active,
        @TchPaging(
            allowedSort = {"locale", "i18nKey", "createdAt", "updatedAt", "level"},
            defaultSort = {"locale,asc", "i18nKey,asc"})
        TchPageRequest pageRequest) {
        var i18nLevel = level == null ? null : I18nOverrideLevel.valueOf(level.toUpperCase());
        SearchI18nOverridesCriteria criteria =
            new SearchI18nOverridesCriteria(i18nLevel, locale, i18nKeyContains, active, null, "active");

        return ApiResponse.success(i18nOverridesCatalog.search(criteria, pageRequest));
    }


    @Operation(summary = "Create a new i18n override")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<I18nOverrideView> create(@RequestBody CreateI18nOverrideRequest request) {
        return ApiResponse.success(adminService.create(request));
    }

    @Operation(summary = "Update an existing i18n override")
    @PutMapping("/{id}")
    public ApiResponse<I18nOverrideView> update(
        @PathVariable I18nOverrideId id, @RequestBody UpdateI18nOverrideRequest request) {
        return ApiResponse.success(adminService.update(id, request));
    }

    @Operation(summary = "Delete an i18n override (soft delete)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable I18nOverrideId id) {
        adminService.delete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Resolve effective i18n overrides for a locale (tenant)")
    @GetMapping("/resolve/{locale}")
    public ApiResponse<Map<String, String>> resolve(
        @PathVariable String locale,
        @CurrentContext TchRequestContext ctx) {

        Map<String, String> map = i18nOverridesCatalog.resolveLocale(locale, ctx);
        return ApiResponse.success(map);
    }

    @Operation(summary = "Global i18n stats (SUPER_ADMIN)")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ApiResponse<I18nGlobalOverviewView> overview() {
        var stats = i18nOverridesCatalog.keyStats();
        return ApiResponse.success(new I18nGlobalOverviewView(
            java.time.Instant.now(),
            new I18nGlobalOverviewView.Summary(stats.totalKeys(), stats.totalLocales(), stats.totalOverrides())));
    }

    @Operation(summary = "Resolve i18n bundle cross-tenant (SUPER_ADMIN)")
    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ApiResponse<Map<String, String>> resolvePlatform(
        @RequestParam String locale,
        @RequestParam(required = false) com.tchalanet.server.common.types.id.TenantId tenantId) {
        Map<String, String> resolved = tenantId == null
            ? i18nOverridesCatalog.resolveLocale(locale)
            : i18nOverridesCatalog.resolveLocaleForTenant(locale, tenantId);
        return ApiResponse.success(resolved);
    }

    public record I18nGlobalOverviewView(java.time.Instant generatedAt, Summary summary) {
        public record Summary(long totalKeys, long totalLocales, long totalOverrides) {}
    }
}
