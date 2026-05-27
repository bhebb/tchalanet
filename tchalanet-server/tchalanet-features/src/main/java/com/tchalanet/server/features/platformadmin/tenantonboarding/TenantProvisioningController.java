package com.tchalanet.server.features.platformadmin.tenantonboarding;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningPreviewView;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningRequest;
import com.tchalanet.server.features.platformadmin.tenantonboarding.model.TenantProvisioningResultView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant provisioning endpoints (dashboard-overview-runtime-v1 §tenant-provisioning).
 *
 *   POST /platform/tenant-onboarding/preview   — read-only, returns what would happen
 *   POST /platform/tenant-onboarding/provision — creates the tenant via owning APIs
 */
@RestController
@RequestMapping("/platform/tenant-onboarding")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Platform Admin • Tenant Onboarding")
public class TenantProvisioningController {

  private final TenantProvisioningOrchestrator orchestrator;

  @PostMapping("/preview")
  @Operation(summary = "Preview the result of provisioning a tenant — read-only")
  public ApiResponse<TenantProvisioningPreviewView> preview(
      @Valid @RequestBody TenantProvisioningRequest request) {
    return ApiResponse.success(orchestrator.preview(request));
  }

  @PostMapping("/provision")
  @Operation(summary = "Provision a new tenant with the given profile")
  public ApiResponse<TenantProvisioningResultView> provision(
      @Valid @RequestBody TenantProvisioningRequest request) {
    return ApiResponse.success(orchestrator.provision(request));
  }
}
