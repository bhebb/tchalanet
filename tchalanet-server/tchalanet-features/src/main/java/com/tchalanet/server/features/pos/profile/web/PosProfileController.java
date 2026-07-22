package com.tchalanet.server.features.pos.profile.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.pos.profile.app.PosProfileService;
import com.tchalanet.server.features.pos.profile.model.PosProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier")
@RequiredArgsConstructor
@PreAuthorize(
    "hasAuthority('ACTOR_SELLER_TERMINAL') or hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Cashier • Profile")
public class PosProfileController {

  private final PosProfileService service;

  @GetMapping("/profile")
  @Operation(summary = "Get compact POS profile and settings context")
  public ApiResponse<PosProfileResponse> profile(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(service.profile(ctx));
  }
}
