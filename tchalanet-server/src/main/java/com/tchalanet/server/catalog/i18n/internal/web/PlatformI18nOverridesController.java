package com.tchalanet.server.catalog.i18n.internal.web;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.internal.web.model.CreateI18nOverrideRequest;
import com.tchalanet.server.catalog.i18n.api.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.internal.web.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.catalog.i18n.internal.web.model.UpdateI18nOverrideRequest;
import com.tchalanet.server.catalog.i18n.internal.write.I18nOverridesAdminService;
import com.tchalanet.server.common.types.id.I18nOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String locale,
      @RequestParam(required = false) String i18nKeyContains,
      @RequestParam(required = false) Boolean active,
      @TchPaging(
              allowedSort = {"locale", "i18nKey", "createdAt", "updatedAt"},
              defaultSort = {"locale,asc", "i18nKey,asc"})
          TchPageRequest pageRequest) {

    SearchI18nOverridesCriteria criteria =
        new SearchI18nOverridesCriteria(
            tenantId != null ? TenantId.of(tenantId) : null, locale, i18nKeyContains, active);

    return ApiResponse.success(i18nOverridesCatalog.search(criteria, pageRequest));
  }

  @Operation(summary = "Get i18n override by ID")
  @GetMapping("/{id}")
  public ApiResponse<I18nOverrideView> getById(@PathVariable I18nOverrideId id) {
    return ApiResponse.success(i18nOverridesCatalog.getById(id));
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
}
