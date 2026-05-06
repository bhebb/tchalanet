package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class SuperAdminTenantsProvider extends StubPageModelDynamicProvider {

  public SuperAdminTenantsProvider() {
    super("superadmin_tenants");
  }
}
