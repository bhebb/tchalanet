package com.tchalanet.server.core.session.internal.infra.scheduler;

import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.session.api.command.CloseDueSalesSessionsCommand;
import com.tchalanet.server.core.session.internal.infra.config.SalesSessionAutoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly auto-close of POS sessions left OPEN past the business day.
 *
 * <p>V1 decision: Tchalanet does NOT auto-open sales sessions. A sales session
 * represents a real seller intent on a terminal/outlet and must be opened explicitly
 * by the cashier. Only auto-close is supported to clean up forgotten sessions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SalesSessionAutoCloseScheduler {

    private static final JobKey SALES_SESSION_AUTO_CLOSE = JobKey.of("session:auto:close");

    private final CommandBus commandBus;
    private final SalesSessionAutoProperties salesSessionAutoProperties;
    private final BatchGate gate;

    @Scheduled(cron = "${tch.session.auto-close.cron:0 10 0 * * *}")
    @TchJob("sales-session:auto-close")
    public void tickClose() {
        if (!canRun()) return;
        commandBus.execute(new CloseDueSalesSessionsCommand());
    }

    private boolean canRun() {
        if (!salesSessionAutoProperties.active()) {
            log.info("sales_session.auto-close skipped reason=scheduler_disabled");
            return false;
        }

        if (!gate.enabled(SALES_SESSION_AUTO_CLOSE, null)) {
            log.info("sales_session.auto-close skipped reason=gate_disabled");
            return false;
        }

        return true;
    }
}
