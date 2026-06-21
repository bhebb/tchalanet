package com.tchalanet.server.features.pos.home.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.pos.home.app.PosHomeService;
import com.tchalanet.server.features.pos.home.app.ClientSurfaceResolver;
import com.tchalanet.server.features.pos.home.model.PosHomeResponse;
import com.tchalanet.server.features.pos.home.model.PosReadinessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Home")
public class PosHomeController {

  private final PosHomeService service;

  @GetMapping("/home")
  @Operation(summary = "Get compact mobile/POS cashier home")
  public ApiResponse<PosHomeResponse> mobileHome(
      @CurrentContext TchRequestContext ctx,
      @RequestHeader(name = ClientSurfaceResolver.HEADER_NAME, required = false) String surface) {
    return ApiResponse.success(service.mobileHome(ctx, surface));
  }

  @GetMapping("/readiness")
  @Operation(summary = "Get cashier readiness and lightweight attention notifications")
  public ApiResponse<PosReadinessResponse> readiness(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(service.readiness(ctx));
  }
}
