package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class SuperAdminVersionProvider extends StubPageModelDynamicProvider {

  public SuperAdminVersionProvider() {
    super("superadmin_version");
  }
}
