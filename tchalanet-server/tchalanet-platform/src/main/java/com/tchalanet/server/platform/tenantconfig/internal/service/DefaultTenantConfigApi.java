package com.tchalanet.server.platform.tenantconfig.internal.service;

import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.platform.tenantconfig.api.model.ActivateTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.CreateTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.GetTenantByCodeQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.GetTenantByIdQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.ListTenantsQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.SuspendTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.TenantConfigView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultTenantConfigApi implements TenantConfigApi {

  private final TenantConfigService service;

  @Override
  public void createTenant(CreateTenantCommand request) {
    service.createTenant(request);
  }

  @Override
  public TenantConfigView getTenantById(GetTenantByIdQuery request) {
    return service.getTenantById(request);
  }

  @Override
  public TenantConfigView getTenantByCode(GetTenantByCodeQuery request) {
    return service.getTenantByCode(request);
  }

  @Override
  public List<TenantConfigView> listTenants(ListTenantsQuery request) {
    return service.listTenants(request).items();
  }

  @Override
  public void activateTenant(ActivateTenantCommand request) {
    service.activateTenant(request);
  }

  @Override
  public void suspendTenant(SuspendTenantCommand request) {
    service.suspendTenant(request);
  }
}
