package com.tchalanet.server.core.offlinesync.internal.infra.scheduler;

import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.command.submission.CloseSyncAcceptedWindowCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@ConditionalOnProperty(prefix = "tch.offlinesync", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SyncAcceptedWindowCloseScheduler {

    private static final String ACTOR = "offlinesync:sync-window-close";

    private final CommandBus commandBus;
    private final TenantPreContextLookupApi tenantRegistry;
    private final JobContextBinder binder;
    private final Clock clock;

    @Scheduled(cron = "${tch.offlinesync.jobs.sync-window-close-cron:0 0 * * * *}", zone = "UTC")
    @SchedulerLock(
        name = "offlinesync_sync_window_close",
        lockAtMostFor = "PT15M",
        lockAtLeastFor = "PT1M")
    public void tick() {
        MDC.put("job", ACTOR);
        try {
            var now = clock.instant();
            for (TenantId tenantId : tenantRegistry.listActiveTenantIds()) {
                try {
                    binder.bindTenant(tenantId, ACTOR);
                    commandBus.execute(new CloseSyncAcceptedWindowCommand(tenantId, now));
                } catch (Exception ex) {
                    log.warn("offlinesync: sync-window-close tenant {} failed — {}",
                        tenantId, ex.getMessage());
                } finally {
                    binder.clear();
                }
            }
        } finally {
            MDC.remove("job");
        }
    }
}
