package com.tchalanet.server.core.outlet.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.BusinessDayOverrideId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.outlet.api.query.OutletBusinessDayOverrideView;
import com.tchalanet.server.core.outlet.internal.application.service.OutletBusinessDayService;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.UpsertOutletBusinessDayOverrideRequest;
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
 * OUTLET-LEVEL business-day closures (one outlet, dated). Owned by core.outlet so
 * the outlet is validated (exists / belongs to tenant / active) before any write.
 *
 * <p>For the whole commerce use {@code /admin/business-days} (tenant-level); for
 * an immediate "this POS is closed now" flag use {@code /admin/outlets/{id}/close-day}.
 */
@RestController
@RequestMapping("/admin/outlets/{outletId}/business-days")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Outlet • Business Days")
public class OutletBusinessDayController {

  private final OutletBusinessDayService service;

  @Operation(summary = "List outlet-level business-day overrides for a date range")
  @GetMapping
  public ApiResponse<List<OutletBusinessDayOverrideView>> list(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId outletId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(
        service.list(ctx.effectiveTenantIdRequired(), outletId, from, to));
  }

  @Operation(summary = "Mark an outlet open/closed on a date (idempotent upsert)")
  @PutMapping
  public ApiResponse<OutletBusinessDayOverrideView> upsert(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId outletId,
      @Valid @RequestBody UpsertOutletBusinessDayOverrideRequest req) {
    return ApiResponse.success(
        service.upsert(
            ctx.effectiveTenantIdRequired(), outletId,
            req.businessDate(), req.open(), req.reasonCode(), req.label()));
  }

  @Operation(summary = "Remove an outlet override (revert to tenant rules / default)")
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId outletId,
      @PathVariable BusinessDayOverrideId id) {
    service.softDelete(ctx.effectiveTenantIdRequired(), outletId, id);
    return ApiResponse.success(null);
  }
}
