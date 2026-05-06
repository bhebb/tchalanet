package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class CashierRecentTicketsProvider extends StubPageModelDynamicProvider {

  public CashierRecentTicketsProvider() {
    super("cashier_recent_tickets");
  }
}
