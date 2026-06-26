package com.tchalanet.server.features.ops.cache;

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

    private static Map<String, List<String>> cacheGroups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("plans", List.of(
            "catalog:plan:active_plans",
            "catalog:plan:plan_by_code",
            "catalog:plan:plan_by_id"));
        return Map.copyOf(groups);
    }

    private static String normalizeGroup(String group) {
        return group == null ? "" : group.trim().toLowerCase(Locale.ROOT);
    }

    public record CacheGroupClearResult(String group, List<String> cleared, List<String> missing) {}
}
