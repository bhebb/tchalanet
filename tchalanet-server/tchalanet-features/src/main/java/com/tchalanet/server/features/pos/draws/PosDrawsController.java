package com.tchalanet.server.features.pos.draws;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier/draws")
@RequiredArgsConstructor
@PreAuthorize(
    "hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Draws")
public class PosDrawsController {

  private final PosDrawsService service;
  private final ResultSlotCatalog resultSlotCatalog;

  @GetMapping("/{drawId}/detail")
  @Operation(
      summary =
          "Draw detail for the cashier — top selections and exposure scoped to the seller"
              + " terminal resolved from context (never from a query param)")
  public ApiResponse<PosDrawDetailResponse> detail(
      @CurrentContext TchRequestContext ctx, @PathVariable UUID drawId) {
    return ApiResponse.success(service.getDetail(ctx, DrawId.of(drawId)));
  }

  @GetMapping("/available")
  @Operation(summary = "List draws available to the cashier for sale (next N hours)")
  public ApiResponse<List<PosAvailableDrawResponse>> available(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(defaultValue = "24") int lookaheadHours,
      @RequestParam(defaultValue = "20") int limit) {
    var draws =
        service.listAvailable(ctx, lookaheadHours, limit).stream()
            .map(draw -> PosAvailableDrawResponse.from(draw, resultSlotCatalog, ctx.tenantZoneId()))
            .toList();
    return ApiResponse.success(draws);
  }
}
