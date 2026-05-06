package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class SuperAdminBatchStatusProvider extends StubPageModelDynamicProvider {

  public SuperAdminBatchStatusProvider() {
    super("superadmin_batch_status");
  }
}
