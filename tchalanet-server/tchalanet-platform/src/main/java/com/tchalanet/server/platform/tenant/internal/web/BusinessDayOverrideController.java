package com.tchalanet.server.platform.tenant.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.BusinessDayOverrideId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.tenant.api.model.BusinessDayOverrideView;
import com.tchalanet.server.platform.tenant.internal.service.BusinessDayOverrideAdminService;
import com.tchalanet.server.platform.tenant.internal.web.model.UpsertBusinessDayOverrideRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * TENANT-LEVEL management of business-day closures. This is how a TENANT_ADMIN says "the whole
 * commerce is closed on date X".
 *
 * <p>Seller-terminal operational availability is not modeled here.
 *
 * <p>Tenant is resolved from the request context (never from client input); RLS enforces isolation.
 */
@RestController
@RequestMapping("/admin/business-days")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Tenant • Business Days")
public class BusinessDayOverrideController {

  private final BusinessDayOverrideAdminService service;

  @Operation(summary = "List tenant-level business-day overrides for a date range")
  @GetMapping
  public ApiResponse<List<BusinessDayOverrideView>> list(
      @CurrentContext TchRequestContext ctx,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(service.list(ctx.tenantIdRequired(), from, to));
  }

  @Operation(summary = "Mark a business day open/closed (idempotent upsert)")
  @PutMapping
  public ApiResponse<BusinessDayOverrideView> upsert(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody UpsertBusinessDayOverrideRequest request) {
    return ApiResponse.success(service.upsert(ctx.tenantIdRequired(), request));
  }

  @Operation(summary = "Remove an override (revert to calendar rules / default)")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @CurrentContext TchRequestContext ctx, @PathVariable BusinessDayOverrideId id) {
    service.softDelete(ctx.tenantIdRequired(), id);
    return ApiResponse.success(null);
  }
}
