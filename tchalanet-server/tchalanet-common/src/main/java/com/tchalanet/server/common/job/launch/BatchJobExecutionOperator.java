package com.tchalanet.server.common.job.launch;

public interface BatchJobExecutionOperator {

    JobStartResult restart(long executionId);
}
