package com.tchalanet.server.common.job.gate;

import com.tchalanet.server.common.exception.TchException;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;

public class BatchDisabledException extends TchException {

    private final String jobKey;

    public BatchDisabledException(String jobKey) {
        super("batch.disabled", "Batch job disabled: " + jobKey);
        this.jobKey = jobKey;
    }

    public BatchDisabledException(JobKey jobKey, TenantId tenantId, String scope) {
        super("batch.disabled", "Batch job disabled: " + jobKey + " tenantId=" + tenantId + " scope=" + scope);
        this.jobKey = jobKey.value();
    }

    public String jobKey() {
        return jobKey;
    }
}
