package com.tchalanet.server.catalog.pricing.internal.infra.web;

import com.tchalanet.server.catalog.pricing.internal.admin.PricingAdminService;
import com.tchalanet.server.catalog.pricing.internal.admin.PricingAdminService.CreatePricingOddsRequest;
import com.tchalanet.server.catalog.pricing.internal.admin.PricingAdminService.UpdatePricingOddsRequest;
import com.tchalanet.server.catalog.pricing.internal.infra.web.model.PricingOddsResponse;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/platform/pricing-odds")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Platform • Pricing Odds")
public class PricingAdminController {

  private final PricingAdminService admin;

  private static PricingOddsResponse toDto(com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity e) {
    return new PricingOddsResponse(
        e.getId(),
        e.getTenantId(),
        e.getGameCode(),
        e.getBetType() == null ? null : e.getBetType().name(),
        e.getBetOption(),
        e.getOdds(),
        e.isActive(),
        e.getCreatedAt(),
        e.getUpdatedAt()
    );
  }

  @Operation(summary = "List active pricing odds (platform)")
  @GetMapping("/active")
  public ApiResponse<List<PricingOddsResponse>> listActive() {
    var list = admin.listActive().stream().map(PricingAdminController::toDto).collect(Collectors.toList());
    return ApiResponse.success(list);
  }

  @Operation(summary = "Get pricing odds by id (platform)")
  @GetMapping("/{id}")
  public ApiResponse<PricingOddsResponse> getById(@PathVariable UUID id) {
    var opt = admin.findById(id).map(PricingAdminController::toDto);
    return ApiResponse.success(opt.orElse(null));
  }

  @Operation(summary = "Create pricing odds (platform)")
  @PostMapping
  public ApiResponse<PricingOddsResponse> create(@RequestBody CreatePricingOddsRequest request) {
    var created = admin.create(request);
    return ApiResponse.created(toDto(created));
  }

  @Operation(summary = "Update pricing odds (platform)")
  @PutMapping("/{id}")
  public ApiResponse<PricingOddsResponse> update(@PathVariable UUID id, @RequestBody UpdatePricingOddsRequest request) {
    var updated = admin.update(id, request);
    return ApiResponse.success(toDto(updated));
  }

  @Operation(summary = "Soft-delete pricing odds (platform)")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    admin.softDelete(id);
    return ApiResponse.success(null);
  }
}
