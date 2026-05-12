package com.tchalanet.server.features.pagemodel.dynamic.providers;

import org.springframework.stereotype.Component;

@Component
public class AdminApprovalQueueProvider extends StubPageModelDynamicProvider {

  public AdminApprovalQueueProvider() {
    super("admin_approval_queue");
  }
}
