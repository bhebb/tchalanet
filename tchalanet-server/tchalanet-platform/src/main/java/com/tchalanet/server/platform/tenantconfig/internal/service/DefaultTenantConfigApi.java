package com.tchalanet.server.platform.tenantconfig.internal.service;

import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.platform.tenantconfig.api.model.request.ActivateTenantRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.ListTenantsRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.SuspendTenantRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantConfigView;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenantconfig.api.model.view.TenantInternalDocumentConfig;
import com.tchalanet.server.platform.tenantconfig.api.model.request.UpdateTenantIdentityRequest;
import com.tchalanet.server.platform.tenantconfig.api.model.request.UpdateTenantInternalSettingsRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultTenantConfigApi implements TenantConfigApi {

  private final TenantConfigService service;

  @Override
  public void createTenant(CreateTenantRequest request) {
    service.createTenant(request);
  }

  @Override
  public TenantConfigView getTenantById(GetTenantByIdRequest request) {
    return service.getTenantById(request);
  }

  @Override
  public TenantConfigView getTenantByCode(GetTenantByCodeRequest request) {
    return service.getTenantByCode(request);
  }

  @Override
  public List<TenantConfigView> listTenants(ListTenantsRequest request) {
    return service.listTenants(request).items();
  }

  @Override
  public void updateTenantIdentity(UpdateTenantIdentityRequest request) {
    service.updateTenantIdentity(request);
  }

  @Override
  public void updateTenantInternalSettings(UpdateTenantInternalSettingsRequest request) {
    service.updateTenantInternalSettings(request);
  }

  @Override
  public void activateTenant(ActivateTenantRequest request) {
    service.activateTenant(request);
  }

  @Override
  public void suspendTenant(SuspendTenantRequest request) {
    service.suspendTenant(request);
  }

  @Override
  public TenantInternalCommunicationConfig getTenantCommunicationConfig(GetTenantByIdRequest request) {
    return service.getTenantCommunicationConfig(request);
  }

  @Override
  public TenantInternalDocumentConfig getTenantDocumentConfig(GetTenantByIdRequest request) {
    return service.getTenantDocumentConfig(request);
  }
}
