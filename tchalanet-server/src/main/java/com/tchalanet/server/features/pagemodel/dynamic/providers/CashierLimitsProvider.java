package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class CashierLimitsProvider extends StubPageModelDynamicProvider {

  public CashierLimitsProvider() {
    super("cashier_limits");
  }
}
