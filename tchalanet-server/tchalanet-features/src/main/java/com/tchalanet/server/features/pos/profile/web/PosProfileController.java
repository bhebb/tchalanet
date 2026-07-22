package com.tchalanet.server.features.pos.profile.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.pos.profile.app.PosProfileService;
import com.tchalanet.server.features.pos.profile.model.PosProfileResponse;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileCommercialRequest;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileSellerRequest;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileTerminalRequest;
import com.tchalanet.server.platform.accesscontrol.api.RequiresPermission;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @PatchMapping("/profile/terminal")
  @RequiresPermission("seller_terminal.manage")
  @Operation(summary = "Update the current POS terminal section")
  @AuditLog(
      entity = AuditEntityType.SELLER_TERMINAL,
      action = AuditAction.SELLER_TERMINAL_UPDATE,
      idExpression = "#ctx.sellerTerminalId().value().toString()",
      detailsExpression = "#request")
  public ApiResponse<PosProfileResponse> updateTerminal(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody UpdatePosProfileTerminalRequest request) {
    return ApiResponse.success(service.updateTerminal(ctx, request));
  }

  @PatchMapping("/profile/seller")
  @Operation(summary = "Update the current POS seller/contact section")
  @AuditLog(
      entity = AuditEntityType.SELLER_TERMINAL,
      action = AuditAction.SELLER_TERMINAL_UPDATE,
      idExpression = "#ctx.sellerTerminalId().value().toString()",
      detailsExpression = "#request")
  public ApiResponse<PosProfileResponse> updateSeller(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody UpdatePosProfileSellerRequest request) {
    return ApiResponse.success(service.updateSeller(ctx, request));
  }

  @PatchMapping("/profile/commercial")
  @RequiresPermission("seller_terminal.manage")
  @Operation(summary = "Update the current POS commercial section")
  @AuditLog(
      entity = AuditEntityType.SELLER_TERMINAL,
      action = AuditAction.SELLER_TERMINAL_UPDATE,
      idExpression = "#ctx.sellerTerminalId().value().toString()",
      detailsExpression = "#request")
  public ApiResponse<PosProfileResponse> updateCommercial(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody UpdatePosProfileCommercialRequest request) {
    return ApiResponse.success(service.updateCommercial(ctx, request));
  }
}
