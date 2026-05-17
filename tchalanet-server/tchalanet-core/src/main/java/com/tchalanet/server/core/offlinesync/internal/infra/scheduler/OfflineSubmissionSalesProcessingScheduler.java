package com.tchalanet.server.core.offlinesync.internal.infra.scheduler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.offlinesync.internal.application.command.model.DispatchReadyOfflineSubmissionsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tch.offlinesync.sales-processing.active", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OfflineSubmissionSalesProcessingScheduler {

    private final CommandBus commandBus;

    @Scheduled(cron = "${tch.offlinesync.sales-processing.cron:0 */5 * * * *}")
    public void dispatchReadySubmissions() {
        log.debug("Offline submission sales processing tick starting");
        var dispatched = commandBus.execute(new DispatchReadyOfflineSubmissionsCommand(null));
        if (dispatched > 0) {
            log.info("Dispatched {} offline submissions to sales", dispatched);
        }
    }
}
