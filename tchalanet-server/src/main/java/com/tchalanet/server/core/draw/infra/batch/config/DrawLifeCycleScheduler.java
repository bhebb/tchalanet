package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.config.batch.BatchWindowsConfig;
import com.tchalanet.server.common.time.DefaultTimeZone;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.port.out.TenantDrawCalendarQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.tchalanet.server.common.batch.key.BatchJobKeys.DRAW_GENERATE;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawLifeCycleScheduler {

    private static final ZoneId DEFAULT_TENANT_ZONE = DefaultTimeZone.AMERICA_NEW_YORK;
    private static final boolean DEFAULT_DRY_RUN = false;

    private final TenantDrawCalendarQueryPort tenantPort;
    private final CommandBus commandBus;
    private final BatchGate batchGate;
    private final BatchWindowsConfig windows;
    private final Clock clock;

    @Scheduled(cron = "0 0 5 * * *", zone = "America/New_York")
    public void generateNext7Days() {
        if (!batchGate.enabled(DRAW_GENERATE, null)) {
            log.info("batch.skip jobKey={} reason=disabled", DRAW_GENERATE);
            return;
        }

        Instant now = Instant.now(clock);
        LocalDate from = now.atZone(DEFAULT_TENANT_ZONE).toLocalDate();
        LocalDate to = from.plusDays(7);

        for (TenantId tenantId : tenantPort.listActiveTenantIdsForDrawCalendar()) {
            try {
                commandBus.send(new GenerateDrawsForRangeCommand(tenantId, from, to, DEFAULT_DRY_RUN, false));
            } catch (Exception e) {
                log.warn("generateNext7Days failed tenantId={} from={} to={}", tenantId, from, to, e);
            }
        }
    }

    @Scheduled(cron = "0 */30 * * * *", zone = "America/New_York")
    public void openWindowed() {
        if (!batchGate.enabled(BatchJobKeys.DRAW_OPEN, null)) return;

        Instant now = Instant.now(clock);
        var nowLocal = now.atZone(DEFAULT_TENANT_ZONE).toLocalTime();
        if (!windows.isInOpenDrawsWindow(nowLocal)) return;

        commandBus.send(new OpenDueDrawsCommand(now, 5000, 24, 12, false));
    }

    @Scheduled(cron = "0 */15 * * * *", zone = "America/New_York")
    public void closeWindowed() {
        if (!batchGate.enabled(BatchJobKeys.DRAW_CLOSE, null)) return;

        Instant now = Instant.now(clock);
        var nowLocal = now.atZone(DEFAULT_TENANT_ZONE).toLocalTime();
        if (!windows.isInCloseDrawsWindow(nowLocal)) return;

        commandBus.send(new CloseDueDrawsCommand(now, 5000, false));
    }
}
