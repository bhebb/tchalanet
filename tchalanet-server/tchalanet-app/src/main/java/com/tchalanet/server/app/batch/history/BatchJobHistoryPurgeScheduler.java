package com.tchalanet.server.app.batch.history;

import com.tchalanet.server.common.job.history.BatchJobHistoryService;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BatchJobHistoryPurgeScheduler {

    private final BatchJobHistoryService historyService;
    private final boolean enabled;
    private final int retentionDays;

    public BatchJobHistoryPurgeScheduler(
        BatchJobHistoryService historyService,
        @Value("${tch.batch.history.purge-enabled:true}") boolean enabled,
        @Value("${tch.batch.history.retention-days:7}") int retentionDays
    ) {
        this.historyService = historyService;
        this.enabled = enabled;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Scheduled(cron = "${tch.batch.history.purge-cron:0 0 4 * * SUN}", zone = "UTC")
    public void purgeOldExecutions() {
        if (!enabled) {
            log.debug("batch.history.purge.skip disabled");
            return;
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        var result = historyService.purgeBefore(cutoff);
        log.info(
            "batch.history.purge.done cutoff={} jobExecutions={} jobInstances={} steps={}",
            result.cutoff(),
            result.jobExecutionRows(),
            result.jobInstanceRows(),
            result.stepExecutionRows());
    }
}
