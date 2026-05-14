package com.tchalanet.server.common.batch.gate;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;

public interface BatchGateFlagStore {
  Optional<Boolean> findTenantFlag(JobKey jobKey, TenantId tenantId);

  Optional<Boolean> findGlobalFlag(JobKey jobKey);
}
