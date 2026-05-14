package com.tchalanet.server.common.job.gate;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;

public interface BatchGate {

  boolean enabled(JobKey jobKey, TenantId tenantId);

  void assertEnabledOrThrow(JobKey jobKey, TenantId tenantId);
}
