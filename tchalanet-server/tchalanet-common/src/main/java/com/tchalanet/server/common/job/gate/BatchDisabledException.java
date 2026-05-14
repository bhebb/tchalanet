package com.tchalanet.server.common.job.gate;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;

public class BatchDisabledException extends RuntimeException {

  private final JobKey jobKey;
  private final TenantId tenantId;
  private final String scope;

  public BatchDisabledException(JobKey jobKey, TenantId tenantId, String scope, String message) {
    super(message);
    this.jobKey = jobKey;
    this.tenantId = tenantId;
    this.scope = scope;
  }

  public BatchDisabledException(JobKey jobKey, TenantId tenantId, String scope) {
    this(jobKey, tenantId, scope, buildMessage(jobKey, tenantId, scope));
  }

  private static String buildMessage(JobKey jobKey, TenantId tenantId, String scope) {
    if (tenantId != null) {
      return String.format(
          "Batch job disabled [scope=%s, jobKey=%s, tenantId=%s]", scope, jobKey, tenantId);
    }
    return String.format("Batch job disabled [scope=%s, jobKey=%s]", scope, jobKey);
  }

  public JobKey jobKey() {
    return jobKey;
  }

  public TenantId tenantId() {
    return tenantId;
  }

  public String scope() {
    return scope;
  }
}
