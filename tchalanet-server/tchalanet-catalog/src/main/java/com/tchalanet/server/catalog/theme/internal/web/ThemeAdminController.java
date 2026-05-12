package com.tchalanet.server.catalog.theme.internal.web;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetStatsView;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.catalog.theme.internal.write.ThemePresetAdminService;
import com.tchalanet.server.catalog.theme.internal.write.ThemePresetAdminService.ThemePresetCreateRequest;
import com.tchalanet.server.catalog.theme.internal.write.ThemePresetAdminService.ThemePresetUpdateRequest;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.common.web.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/theme-presets")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class ThemeAdminController {

    private final ThemePresetAdminService admin;
    private final ThemeCatalog themeCatalog;

    @GetMapping
    public ApiResponse<List<ThemePresetView>> listActive() {
        return ApiResponse.success(themeCatalog.listActive());
    }

    @GetMapping("/overview")
    public ApiResponse<ThemePresetStatsView> overview() {
        return ApiResponse.success(themeCatalog.stats());
    }

    @PostMapping
    public ApiResponse<ThemePresetView> create(@RequestBody ThemePresetCreateRequest req) {
        var created = admin.create(req);
        return ApiResponse.created(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<ThemePresetView> update(@PathVariable ThemePresetId id, @RequestBody ThemePresetUpdateRequest req) {
        var updated = admin.update(id, req);
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable ThemePresetId id) {
        admin.softDelete(id);
        return ApiResponse.success(null);
    }
}
