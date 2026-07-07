package com.tchalanet.server.features.ops.cache;

import com.tchalanet.server.common.cache.CacheToggle;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ops controller for cache management (list, clear by name, clear all).
 * NOTE: /api/v1 prefix is added automatically by platform config.
 * Endpoints:
 * - GET    /platform/ops/cache
 * - DELETE /platform/ops/cache/{cacheName}
 * - DELETE /platform/ops/cache
 */
@RestController
@RequestMapping("/platform/ops/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Ops • Cache")
public class CacheOpsController {

    private static final Map<String, List<String>> CACHE_GROUPS = cacheGroups();

    private final CacheManager cacheManager;
    private final CacheToggle cacheToggle;

    @GetMapping
    @Operation(summary = "List all cache regions")
    public ApiResponse<List<CacheView>> listCaches() {
        var names = new ArrayList<>(cacheManager.getCacheNames());
        Collections.sort(names);
        var views = names.stream()
            .map(name -> new CacheView(name, 0L, null, null))
            .toList();
        return ApiResponse.success(views);
    }

    @DeleteMapping("/groups/{group}")
    @Operation(summary = "Clear a named cache group")
    @AuditLog(
        entity = AuditEntityType.SYSTEM,
        action = AuditAction.CACHE_CLEAR,
        idExpression = "'group:' + #group",
        detailsExpression = "#reason")
    public ApiResponse<CacheGroupClearResult> clearCacheGroup(
        @PathVariable String group,
        @RequestParam(required = false) String reason) {
        String normalizedGroup = normalizeGroup(group);
        List<String> cacheNames = CACHE_GROUPS.get(normalizedGroup);
        if (cacheNames == null) {
            return ApiResponse.success(new CacheGroupClearResult(normalizedGroup, List.of(), List.of()));
        }

        List<String> cleared = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        cacheNames.forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache == null) {
                missing.add(name);
            } else {
                cache.clear();
                cleared.add(name);
            }
        });
        return ApiResponse.success(new CacheGroupClearResult(normalizedGroup, List.copyOf(cleared), List.copyOf(missing)));
    }

    @DeleteMapping("/{cacheName}")
    @Operation(summary = "Clear a cache region by name")
    @AuditLog(
        entity = AuditEntityType.SYSTEM,
        action = AuditAction.CACHE_CLEAR,
        idExpression = "#cacheName",
        detailsExpression = "#cacheName")
    public ApiResponse<Void> clearCache(
        @PathVariable String cacheName,
        @RequestParam(required = false) String reason) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
        return ApiResponse.success(null);
    }

    @DeleteMapping
    @Operation(summary = "Clear all cache regions")
    @AuditLog(
        entity = AuditEntityType.SYSTEM,
        action = AuditAction.CACHE_CLEAR,
        idExpression = "'all-caches'",
        detailsExpression = "#reason")
    public ApiResponse<Void> clearAllCaches(
        @RequestParam(required = false) String reason) {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        return ApiResponse.success(null);
    }

    @GetMapping("/toggles")
    @Operation(summary = "List caches currently disabled via the runtime kill-switch")
    public ApiResponse<List<String>> listDisabledCaches() {
        var disabled = new ArrayList<>(cacheToggle.disabledCaches());
        Collections.sort(disabled);
        return ApiResponse.success(disabled);
    }

    @PostMapping("/{cacheName}/disable")
    @Operation(summary = "Disable a cache at runtime (reads miss, writes dropped) without redeploy")
    @AuditLog(
        entity = AuditEntityType.SYSTEM,
        action = AuditAction.CACHE_CLEAR,
        idExpression = "#cacheName",
        detailsExpression = "'disable ' + #cacheName + (#reason == null ? '' : ' — ' + #reason)")
    public ApiResponse<Void> disableCache(
        @PathVariable String cacheName,
        @RequestParam(required = false) String reason) {
        cacheToggle.disable(cacheName);
        return ApiResponse.success(null);
    }

    @PostMapping("/{cacheName}/enable")
    @Operation(summary = "Re-enable a previously disabled cache")
    @AuditLog(
        entity = AuditEntityType.SYSTEM,
        action = AuditAction.CACHE_CLEAR,
        idExpression = "#cacheName",
        detailsExpression = "'enable ' + #cacheName")
    public ApiResponse<Void> enableCache(
        @PathVariable String cacheName,
        @RequestParam(required = false) String reason) {
        cacheToggle.enable(cacheName);
        return ApiResponse.success(null);
    }

    private static Map<String, List<String>> cacheGroups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("catalog", List.of(
            "catalog:plan:active_plans", "catalog:plan:plan_by_code", "catalog:plan:plan_by_id",
            "catalog:game:active_games", "catalog:game:game_by_code", "catalog:game:game_by_id",
            "catalog:theme:active_presets", "catalog:theme:preset_by_code",
            "catalog:resultslot:v2:active", "catalog:resultslot:v2:by_key", "catalog:resultslot:v2:by_id",
            "catalog:resultslot:calendar:v1:by_slot",
            "catalog:pagemodeltemplate:by_id", "catalog:pagemodeltemplate:by_logicalId",
            "catalog:pagemodeltemplate:visible_list", "catalog:pagemodeltemplate:search"));
        groups.put("tenant-config", List.of(
            "catalog:drawchannel:by_tenant", "catalog:drawchannel:by_id",
            "catalog:drawchannel:by_tenant_game_map", "catalog:drawchannel:calendar_rows",
            "catalog:settings:resolved_settings", "catalog:i18n:resolved_by_locale",
            "platform:tenanttheme:runtime", "platform:tenantgame:runtime", "platform:tenantgame:projection",
            "platform.tenant.cache.REGISTRY_BY_CODE", "platform.tenant.cache.REGISTRY_BY_ID",
            "platform.tenant.cache.ACTIVE_TENANT_IDS"));
        groups.put("pricing", List.of(
            "core:pricing:tenant_odds_list",
            "core:pricing:tenant_odds_by_variant"));
        groups.put("drawresult", List.of(
            "core.drawresult.by_id", "core.drawresult.id.by_slot_occurred"));
        return Map.copyOf(groups);
    }

    private static String normalizeGroup(String group) {
        return group == null ? "" : group.trim().toLowerCase(Locale.ROOT);
    }

    public record CacheGroupClearResult(String group, List<String> cleared, List<String> missing) {}
}
