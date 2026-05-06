package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class CashierSessionProvider extends StubPageModelDynamicProvider {

  public CashierSessionProvider() {
    super("cashier_session");
  }
}
