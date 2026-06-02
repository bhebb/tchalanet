package com.tchalanet.server.core.offlinesync.internal.infra.scheduler;

import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.command.submission.ReleaseOrphanedReservedCodeCommand;
import com.tchalanet.server.core.offlinesync.api.model.code.OfflineCodeStatus;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineCodeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "tch.offlinesync", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OrphanedCodeReservationScheduler {

    private static final String ACTOR = "offlinesync:orphaned-codes";

    private final OfflineCodeJpaRepository codeRepo;
    private final CommandBus commandBus;
    private final TenantPreContextLookupApi tenantRegistry;
    private final JobContextBinder binder;
    private final Clock clock;

    @Scheduled(cron = "${tch.offlinesync.jobs.orphaned-code-cron:0 */15 * * * *}", zone = "UTC")
    @SchedulerLock(
        name = "offlinesync_orphaned_codes",
        lockAtMostFor = "PT15M",
        lockAtLeastFor = "PT30S")
    public void tick() {
        MDC.put("job", ACTOR);
        try {
            var threshold = clock.instant().minus(Duration.ofMinutes(30));
            for (TenantId tenantId : tenantRegistry.listActiveTenantIds()) {
                try {
                    binder.bindTenant(tenantId, ACTOR);
                    var orphaned = codeRepo.findAllByStatusAndReservedAtLessThan(
                        OfflineCodeStatus.RESERVED.name(), threshold);
                    for (var c : orphaned) {
                        try {
                            commandBus.execute(new ReleaseOrphanedReservedCodeCommand(
                                tenantId, OfflineCodeId.of(c.getId())));
                        } catch (Exception ex) {
                            log.warn("offlinesync: failed to release code {} tenant={} — {}",
                                c.getId(), tenantId, ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("offlinesync: orphaned-codes tenant {} failed — {}",
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
