package com.tchalanet.server.catalog.settings.internal.web;

import com.tchalanet.server.catalog.settings.api.*;
import com.tchalanet.server.catalog.settings.internal.web.model.CreateSettingRequest;
import com.tchalanet.server.catalog.settings.internal.web.model.SearchSettingsCriteria;
import com.tchalanet.server.catalog.settings.internal.web.model.UpdateSettingRequest;
import com.tchalanet.server.catalog.settings.internal.write.SettingsAdminService;
import com.tchalanet.server.common.types.id.SettingId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Platform Settings Admin Controller
 *
 * <p>Provides CRUD operations for platform administrators to manage application settings.
 *
 * <p>Security: PLATFORM_ADMIN role required (configured in SecurityConfig).
 *
 * <p>This controller delegates all logic to {@link SettingsAdminService} and returns {@link
 * ApiResponse} wrappers.
 */
@RestController
@RequestMapping("/platform/settings")
@RequiredArgsConstructor
@Tag(name = "Platform • Settings", description = "Platform admin CRUD for application settings")
public class PlatformSettingsController {

  private final SettingsAdminService adminService;

  @Operation(summary = "Search settings (paginated)", description = "Search settings with filters and pagination")
  @GetMapping
  public ApiResponse<TchPage<SettingView>> search(
      @RequestParam(required = false) String namespace,
      @RequestParam(required = false) String settingKey,
      @RequestParam(required = false) SettingLevel level,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) Boolean active,
      TchPageRequest pageRequest) {

    SearchSettingsCriteria criteria =
        new SearchSettingsCriteria(
            namespace,
            settingKey,
            level,
            tenantId != null ? com.tchalanet.server.common.types.id.TenantId.of(tenantId) : null,
            active);

    return ApiResponse.success(adminService.search(criteria, pageRequest));
  }

  @Operation(summary = "Get setting by ID")
  @GetMapping("/{id}")
  public ApiResponse<SettingView> getById(@PathVariable SettingId id) {
    return ApiResponse.success(adminService.getById(id));
  }

  @Operation(summary = "Create a new setting")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<SettingView> create(@RequestBody CreateSettingRequest request) {
    return ApiResponse.success(adminService.create(request));
  }

  @Operation(summary = "Update an existing setting")
  @PutMapping("/{id}")
  public ApiResponse<SettingView> update(
      @PathVariable SettingId id, @RequestBody UpdateSettingRequest request) {
    return ApiResponse.success(adminService.update(id, request));
  }

  @Operation(summary = "Delete a setting (soft delete)")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ApiResponse<Void> delete(@PathVariable SettingId id) {
    adminService.delete(id);
    return ApiResponse.success(null);
  }
}
