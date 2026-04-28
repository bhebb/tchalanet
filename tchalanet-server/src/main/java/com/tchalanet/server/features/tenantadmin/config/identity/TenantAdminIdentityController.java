package com.tchalanet.server.features.tenantadmin.config.identity;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.tenantconfig.application.command.model.UpdateTenantIdentityCommand;
import com.tchalanet.server.core.tenantconfig.application.query.model.GetTenantByIdQuery;
import com.tchalanet.server.core.tenantconfig.application.query.model.TenantConfigView;
import com.tchalanet.server.features.tenantadmin.config.model.TenantIdentityView;
import com.tchalanet.server.features.tenantadmin.config.model.UpdateTenantIdentityRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config/identity")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminIdentityController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @GetMapping
  public ApiResponse<TenantIdentityView> getIdentity(@CurrentContext TchRequestContext ctx) {
    var tenant = queryBus.send(new GetTenantByIdQuery(ctx.tenantIdSafe()));
    return ApiResponse.success(toView(tenant));
  }

  @PutMapping
  public ApiResponse<TenantIdentityView> updateIdentity(
      @CurrentContext TchRequestContext ctx, @Valid @RequestBody UpdateTenantIdentityRequest req) {
    var tenantId = ctx.tenantIdSafe();
    commandBus.send(new UpdateTenantIdentityCommand(tenantId, req.name(), req.timeZone(), req.currency()));
    var tenant = queryBus.send(new GetTenantByIdQuery(tenantId));
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
