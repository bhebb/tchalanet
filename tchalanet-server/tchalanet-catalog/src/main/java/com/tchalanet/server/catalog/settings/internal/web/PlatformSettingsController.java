package com.tchalanet.server.catalog.settings.internal.web;

import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.settings.api.model.SearchSettingsAdminCriteria;
import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.model.SettingsCatalogStatsView;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.catalog.settings.internal.web.model.CreateSettingRequest;
import com.tchalanet.server.catalog.settings.internal.web.model.UpdateSettingRequest;
import com.tchalanet.server.catalog.settings.internal.write.SettingsAdminService;
import com.tchalanet.server.common.types.id.SettingId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Platform Settings Admin Controller
 *
 * <p>Uses {@link SearchSettingsAdminCriteria} from api/ to avoid coupling controller
 * to internal web models.
 */
@RestController
@RequestMapping("/platform/settings")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Platform • Settings", description = "Platform admin CRUD for application settings")
public class PlatformSettingsController {

  private final SettingsAdminService adminService;
  private final SettingsCatalog settingsCatalog;

  @Operation(summary = "Search settings (paginated)")
  @GetMapping
  public ApiResponse<TchPage<SettingView>> search(
      @RequestParam(required = false) String namespace,
      @RequestParam(required = false) String settingKey,
      @RequestParam(required = false) SettingLevel level,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) Boolean active,
      TchPageRequest pageRequest) {

    var criteria = new SearchSettingsAdminCriteria(
        namespace,
        settingKey,
        level,
        tenantId != null ? TenantId.of(tenantId) : null,
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
  public ApiResponse<SettingView> create(@Valid @RequestBody CreateSettingRequest request) {
    return ApiResponse.success(adminService.create(request));
  }

  @Operation(summary = "Update an existing setting")
  @PutMapping("/{id}")
  public ApiResponse<SettingView> update(
      @PathVariable SettingId id, @Valid @RequestBody UpdateSettingRequest request) {
    return ApiResponse.success(adminService.update(id, request));
  }

  @Operation(summary = "Delete a setting (soft delete)")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ApiResponse<Void> delete(@PathVariable SettingId id) {
    adminService.delete(id);
    return ApiResponse.success(null);
  }

  @Operation(summary = "Global settings stats")
  @GetMapping("/overview")
  public ApiResponse<SettingsCatalogStatsView> overview() {
    return ApiResponse.success(settingsCatalog.stats());
  }
}
