package com.tchalanet.server.features.tenantadmin.tenant;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.address.api.model.AddressInput;
import com.tchalanet.server.platform.address.api.model.AddressView;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantIdentityRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/admin/tenant")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Tenant Admin • Tenant")
public class AdminTenantController {

  private final AddressApi addressApi;
  private final TenantConfigApi tenantConfigApi;

  @GetMapping("/address")
  @Operation(summary = "Get primary address of the current tenant")
  public ApiResponse<Optional<AddressView>> getAddress(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(addressApi.findPrimaryByTenantId(ctx.tenantId()));
  }

  @PutMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Update identity fields of the current tenant")
  public void updateIdentity(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody UpdateTenantIdentityBody body) {
    tenantConfigApi.updateTenantIdentity(
        new UpdateTenantIdentityRequest(ctx.tenantId(), body.name(), body.timezone(), body.currency()));
  }

  @PutMapping("/address")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Upsert primary address of the current tenant")
  public void upsertAddress(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody UpsertTenantAddressRequest req) {
    addressApi.upsertTenantPrimary(
        ctx.tenantId(),
        new AddressInput(req.line1(), req.line2(), req.city(), req.region(), req.country(), req.postalCode()));
  }

  public record UpdateTenantIdentityBody(String name, String timezone, String currency) {}
}
