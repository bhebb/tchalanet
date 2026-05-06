package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class AdminAlertsProvider extends StubPageModelDynamicProvider {

  public AdminAlertsProvider() {
    super("admin_alerts");
  }
}
