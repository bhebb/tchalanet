package com.tchalanet.server.catalog.pricing.internal.web;

import com.tchalanet.server.catalog.pricing.internal.write.PricingAdminService;
import com.tchalanet.server.catalog.pricing.internal.web.model.CreatePricingOddsRequest;
import com.tchalanet.server.catalog.pricing.internal.web.model.UpdatePricingOddsRequest;
import com.tchalanet.server.catalog.pricing.internal.web.model.PricingOddsView;
import com.tchalanet.server.common.types.id.PricingOddsId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Platform controller for Pricing odds. Returns UI-friendly views.
 * Platform-level reference data: SUPER_ADMIN only, under /platform.
 */
@Tag(name = "Platform • Pricing", description = "Platform CRUD for pricing odds definitions")
@RestController
@RequestMapping("/platform/pricing")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PricingAdminController {

  private final PricingAdminService adminService;

  @Operation(summary = "List active pricing odds")
  @GetMapping
  public ApiResponse<List<PricingOddsView>> listActive() {
    List<PricingOddsView> rows = adminService.listActive();
    return ApiResponse.success(rows);
  }

  @Operation(summary = "Find pricing odds by id")
  @GetMapping("/{id}")
  public ApiResponse<PricingOddsView> findById(@PathVariable PricingOddsId id) {
    var opt = adminService.findById(id);
    return ApiResponse.success(opt.orElse(null));
  }

  @Operation(summary = "Create new pricing odds")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<PricingOddsView> create(@Valid @RequestBody CreatePricingOddsRequest req) {
    var saved = adminService.create(req);
    return ApiResponse.success(saved);
  }

  @Operation(summary = "Update existing pricing odds")
  @PutMapping("/{id}")
  public ApiResponse<PricingOddsView> update(@PathVariable PricingOddsId id, @Valid @RequestBody UpdatePricingOddsRequest req) {
    var updated = adminService.update(id, req);
    return ApiResponse.success(updated);
  }

  @Operation(summary = "Soft-delete pricing odds")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ApiResponse<Void> delete(@PathVariable PricingOddsId id) {
    adminService.softDelete(id);
    return ApiResponse.success(null);
  }
}
