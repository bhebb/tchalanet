package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class AdminKpisProvider extends StubPageModelDynamicProvider {

  public AdminKpisProvider() {
    super("admin_kpis");
  }
}
