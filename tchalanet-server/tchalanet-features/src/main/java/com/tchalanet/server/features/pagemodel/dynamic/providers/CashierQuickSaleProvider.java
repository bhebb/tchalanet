package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class CashierQuickSaleProvider extends StubPageModelDynamicProvider {

  public CashierQuickSaleProvider() {
    super("cashier_quick_sale");
  }
}
