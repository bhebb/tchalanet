package com.tchalanet.server.features.privatedashboard;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.privatedashboard.block.PrivateDashboardDynamicPayload;
import com.tchalanet.server.features.privatedashboard.dynamic.PrivateDashboardDynamicDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/private/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tenant • Dashboard")
public class PrivateDashboardController {

  private final PrivateDashboardService service;
  private final PrivateDashboardDynamicDataService dynamicDataService;

  @Operation(summary = "Get private dashboard for current user (tenant)")
  @GetMapping
  public ResponseEntity<ApiResponse<PrivateDashboardResponse>> getDashboard(
      @RequestParam(name = "lang", required = false) String lang,
      @RequestHeader(value = "X-User-Id", required = false) UUID userId,
      @RequestHeader(value = "X-User-Lang", required = false) String userPreferredLang) {
    UUID effectiveUserId = userId != null ? userId : UUID.randomUUID();

    ApiResponse<PrivateDashboardResponse> response =
        service.getDashboard(
            Optional.ofNullable(lang), UserId.of(effectiveUserId), userPreferredLang);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get tenant dashboard for superadmin (tenant view)")
  @GetMapping("/{tenantId}")
  public ResponseEntity<ApiResponse<PrivateDashboardDynamicPayload>>
      getTenantDashboardForSuperadmin(
          @PathVariable TenantId tenantId,
          @RequestParam(name = "lang", required = false) String lang,
          @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
    UUID effectiveUserId = userId != null ? userId : UUID.randomUUID();
    ApiResponse<PrivateDashboardDynamicPayload> response =
        service.getTenantDashboardForSuperadmin(
            tenantId, Optional.ofNullable(lang), UserId.of(effectiveUserId));
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get cashier dashboard for superadmin (tenant view)")
  @GetMapping("/tenant/{tenantId}/cashier/{cashierId}")
  public ResponseEntity<ApiResponse<PrivateDashboardDynamicPayload>>
      getCashierDashboardForSuperadmin(
          @PathVariable TenantId tenantId,
          @PathVariable UserId cashierId,
          @RequestParam(name = "lang", required = false) String lang,
          @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
    var effectiveUserId = userId != null ? userId : cashierId.uuid();
    var response =
        service.getCashierDashboardForSuperadmin(
            tenantId,
            UserId.of(cashierId.uuid()),
            Optional.ofNullable(lang),
            UserId.of(effectiveUserId));
    return ResponseEntity.ok(response);
  }
}
