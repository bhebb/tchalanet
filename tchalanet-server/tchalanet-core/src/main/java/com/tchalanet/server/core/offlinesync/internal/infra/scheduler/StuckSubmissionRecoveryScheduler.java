package com.tchalanet.server.core.offlinesync.internal.infra.scheduler;

import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.command.submission.RecoverStuckOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "tch.offlinesync", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StuckSubmissionRecoveryScheduler {

    private static final String ACTOR = "offlinesync:stuck-recovery";

    private final OfflineSubmissionJpaRepository submissionRepo;
    private final CommandBus commandBus;
    private final TenantPreContextLookupApi tenantRegistry;
    private final JobContextBinder binder;
    private final Clock clock;

    @Scheduled(cron = "${tch.offlinesync.jobs.stuck-recovery-cron:0 */10 * * * *}", zone = "UTC")
    @SchedulerLock(
        name = "offlinesync_stuck_recovery",
        lockAtMostFor = "PT10M",
        lockAtLeastFor = "PT30S")
    public void tick() {
        MDC.put("job", ACTOR);
        try {
            var threshold = clock.instant().minus(Duration.ofMinutes(15));
            for (TenantId tenantId : tenantRegistry.listActiveTenantIds()) {
                try {
                    binder.bindTenant(tenantId, ACTOR);
                    var stuck = submissionRepo.findAllByStatusInAndPromotionRequestedAtLessThan(
                        List.of(
                            OfflineSubmissionStatus.PROMOTION_REQUESTED.name(),
                            OfflineSubmissionStatus.TECH_VALIDATED.name()),
                        threshold);
                    for (var s : stuck) {
                        try {
                            commandBus.execute(new RecoverStuckOfflineSubmissionCommand(
                                tenantId, OfflineSubmissionId.of(s.getId())));
                        } catch (Exception ex) {
                            log.warn("offlinesync: failed to recover submission {} tenant={} — {}",
                                s.getId(), tenantId, ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("offlinesync: stuck-recovery tenant {} failed — {}",
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
