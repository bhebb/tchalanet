package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class CashierNextDrawsProvider extends StubPageModelDynamicProvider {

  public CashierNextDrawsProvider() {
    super("cashier_next_draws");
  }
}
