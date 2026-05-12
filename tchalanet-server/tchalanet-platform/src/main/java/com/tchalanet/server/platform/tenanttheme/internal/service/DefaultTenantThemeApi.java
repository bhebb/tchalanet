package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.platform.tenanttheme.api.TenantThemeApi;
import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeCommand;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeCommand;
import com.tchalanet.server.platform.tenanttheme.api.model.ResolveTenantThemeQuery;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultTenantThemeApi implements TenantThemeApi {

  private final TenantThemeService service;

  @Override
  public void applyTenantTheme(ApplyTenantThemeCommand request) {
    service.applyTenantTheme(request);
  }

  @Override
  public TenantThemeView resolveTenantTheme(ResolveTenantThemeQuery request) {
    return service.resolveTenantTheme(request);
  }

  @Override
  public void deactivateTenantTheme(DeactivateTenantThemeCommand request) {
    service.deactivateTenantTheme(request);
  }
}
