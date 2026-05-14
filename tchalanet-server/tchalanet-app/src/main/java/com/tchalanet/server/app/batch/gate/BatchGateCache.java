package com.tchalanet.server.app.batch.gate;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;

public interface BatchGateCache {

  Optional<Boolean> getTenant(JobKey jobKey, TenantId tenantId);

  Optional<Boolean> getGlobal(JobKey jobKey);

  void cacheTenant(JobKey jobKey, TenantId tenantId, boolean enabled);

  void cacheGlobal(JobKey jobKey, boolean enabled);

  void evictTenant(JobKey jobKey, TenantId tenantId);

  void evictGlobal(JobKey jobKey);

  void evictAll();
}
