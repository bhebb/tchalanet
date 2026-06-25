package com.tchalanet.server.common.job.history;

import com.tchalanet.server.common.job.key.JobKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BatchJobHistoryService {

    Optional<BatchJobExecutionView> getExecution(long executionId);

    List<BatchJobExecutionView> listExecutions(JobKey jobKey, int limit);

    BatchJobHistoryPurgeResult purgeBefore(Instant cutoff);
}
