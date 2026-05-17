package com.tchalanet.server.features.tenantadmin.config.identity;

import com.tchalanet.server.common.context.web.CurrentContext;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.tenantconfig.api.model.request.UpdateTenantIdentityRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantConfigView;
import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.features.tenantadmin.config.model.TenantIdentityView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config/identity")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminIdentityController {

  private final TenantConfigApi tenantConfigApi;

  @GetMapping
  public ApiResponse<TenantIdentityView> getIdentity(@CurrentContext TchRequestContext ctx) {
    var tenant = tenantConfigApi.getTenantById(new GetTenantByIdRequest(ctx.tenantIdSafe()));
    return ApiResponse.success(toView(tenant));
  }

  @PutMapping
  public ApiResponse<TenantIdentityView> updateIdentity(
      @CurrentContext TchRequestContext ctx, @Valid @RequestBody UpdateTenantIdentityRequest req) {
    var tenantId = ctx.tenantIdSafe();
    tenantConfigApi.updateTenantIdentity(
        new UpdateTenantIdentityRequest(tenantId, req.name(), req.timezone(), req.currency()));
    var tenant = tenantConfigApi.getTenantById(new GetTenantByIdRequest(tenantId));
    return ApiResponse.success(toView(tenant));
  }

  private static TenantIdentityView toView(TenantConfigView tenant) {
    return new TenantIdentityView(
        tenant.tenantId().value().toString(),
        tenant.code(),
        tenant.name(),
        tenant.timezone() == null ? null : tenant.timezone().toString(),
        tenant.currency() == null ? null : tenant.currency().getCurrencyCode(),
        tenant.status().name(),
        tenant.type().name());
  }
}
