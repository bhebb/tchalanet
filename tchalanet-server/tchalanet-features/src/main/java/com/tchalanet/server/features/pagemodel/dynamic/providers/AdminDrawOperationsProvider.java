package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class AdminDrawOperationsProvider extends StubPageModelDynamicProvider {

  public AdminDrawOperationsProvider() {
    super("admin_draw_operations");
  }
}
