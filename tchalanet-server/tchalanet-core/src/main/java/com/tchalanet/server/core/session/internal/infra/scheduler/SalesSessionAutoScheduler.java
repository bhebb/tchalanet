package com.tchalanet.server.core.session.internal.infra.scheduler;

import com.tchalanet.server.common.batch.annotation.BatchScheduledJob;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.session.api.command.CloseDueSalesSessionsCommand;
import com.tchalanet.server.core.session.api.command.OpenDueSalesSessionsCommand;
import com.tchalanet.server.core.session.internal.infra.config.SalesSessionAutoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.tchalanet.server.common.batch.key.BatchJobKeys.SALES_SESSION_AUTO;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesSessionAutoScheduler {

    private final CommandBus commandBus;
    private final SalesSessionAutoProperties salesSessionAutoProperties;
    private final BatchGate gate;

    @Scheduled(cron = "${tch.session.auto.open-cron:0 0 5 * * *}")
    @BatchScheduledJob("sales-session:auto-open")
    public void tickOpen() {
        if (!canRunSalesSessionAuto("open")) return;
        commandBus.execute(new OpenDueSalesSessionsCommand());
    }

    @Scheduled(cron = "${tch.session.auto.close-cron:0 0 20 * * *}")
    @BatchScheduledJob("sales-session:auto-close")
    public void tickClose() {
        if (!canRunSalesSessionAuto("close")) return;
        commandBus.execute(new CloseDueSalesSessionsCommand());
    }

    private boolean canRunSalesSessionAuto(String action) {
        if (!salesSessionAutoProperties.active()) {
            log.info("sales_session.auto.{} skipped reason=scheduler_disabled", action);
            return false;
        }

        if (!gate.enabled(SALES_SESSION_AUTO, null)) {
            log.info("sales_session.auto.{} skipped reason=gate_disabled", action);
            return false;
        }

        return true;
    }
}
