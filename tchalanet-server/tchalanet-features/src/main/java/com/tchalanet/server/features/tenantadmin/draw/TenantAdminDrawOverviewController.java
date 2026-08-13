package com.tchalanet.server.features.tenantadmin.draw;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasPermission(null, 'draw.read')")
@Tag(name = "Tenant Admin • Draws")
public class TenantAdminDrawOverviewController {

  private final TenantAdminDrawOverviewService service;

  @GetMapping("/{drawId}/overview")
  @Operation(
      summary =
          "Draw overview — BFF aggregating draw info, top selections, effective limits and"
              + " exposure alerts for the admin draw detail page")
  public ApiResponse<AdminDrawOverviewResponse> overview(
      @CurrentContext TchRequestContext ctx, @PathVariable UUID drawId) {
    return ApiResponse.success(service.getOverview(ctx, DrawId.of(drawId)));
  }
}
