package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class SuperAdminSystemHealthProvider extends StubPageModelDynamicProvider {

  public SuperAdminSystemHealthProvider() {
    super("superadmin_system_health");
  }
}
