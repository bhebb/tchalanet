package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.platform.tenanttheme.api.TenantThemeApi;
import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.ResolveTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultTenantThemeApi implements TenantThemeApi {

  private final TenantThemeService service;

  @Override
  public void applyTenantTheme(ApplyTenantThemeRequest request) {
    service.applyTenantTheme(request);
  }

  @Override
  public TenantThemeView resolveTenantTheme(ResolveTenantThemeRequest request) {
    return service.resolveTenantTheme(request);
  }

  @Override
  public void deactivateTenantTheme(DeactivateTenantThemeRequest request) {
    service.deactivateTenantTheme(request);
  }
}
