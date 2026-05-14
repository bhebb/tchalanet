package com.tchalanet.server.app.batch.gate;

import com.tchalanet.server.common.job.gate.BatchDisabledException;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchGateImpl implements BatchGate {

  private final BatchGateResolver resolver;

  @Override
  public boolean enabled(JobKey jobKey, TenantId tenantId) {
    if (jobKey == null) throw new IllegalArgumentException("jobKey required");
    return resolver.resolve(jobKey, tenantId);
  }

  @Override
  public void assertEnabledOrThrow(JobKey jobKey, TenantId tenantId) {
    if (jobKey == null) throw new IllegalArgumentException("jobKey required");

    var res = resolver.resolveWithScope(jobKey, tenantId);
    if (!res.enabled()) {
      log.warn("batch.disabled jobKey={} tenantId={} source={}", jobKey, tenantId, res.scope());
      throw new BatchDisabledException(jobKey, tenantId, res.scope());
    }
  }
}
