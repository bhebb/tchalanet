package com.tchalanet.server.core.session.internal.application.command.handler;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.session.api.command.CloseDueSalesSessionsCommand;
import com.tchalanet.server.core.session.api.command.CloseDueSalesSessionsResult;
import com.tchalanet.server.core.session.internal.application.port.out.AutoSessionTargetReaderPort;
import com.tchalanet.server.core.session.internal.application.service.SalesSessionAutoCloser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Nightly auto-close of POS sessions left OPEN past the business day.
 *
 * <p>V1 rule: closes only sessions whose {@code businessDate} is strictly before the
 * tenant-local today. The current business day is never touched. Idempotent: returns
 * {@code closedCount = 0} when no eligible sessions exist.
 *
 * <p>Iterates over all active tenants and binds the RLS context per tenant so that
 * the SQL query is correctly scoped — mirrors the pattern used in
 * {@code DrawLifeCycleTickScheduler}.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CloseDueSalesSessionsCommandHandler
    implements CommandHandler<CloseDueSalesSessionsCommand, CloseDueSalesSessionsResult> {

    private static final String CLOSE_REASON = "AUTO_CLOSE_END_OF_DAY";
    private static final String ACTOR        = "session-auto-close";

    private final AutoSessionTargetReaderPort autoSessionTargetReader;
    private final SalesSessionAutoCloser autoCloser;
    private final TenantCatalog tenantCatalog;
    private final JobContextBinder jobContextBinder;
    private final Clock clock;

    @Override
    @TchTx
    public CloseDueSalesSessionsResult handle(CloseDueSalesSessionsCommand command) {
        var now = clock.instant();
        int totalTargets = 0;
        int totalClosed  = 0;

        for (TenantId tenantId : tenantCatalog.listActiveTenantIds()) {
            try {
                jobContextBinder.bindTenant(tenantId, ACTOR);

                // Compute tenant-local today using the timezone stored on the tenant.
                // Sessions with businessDate < tenantToday belong to a past business day.
                var zone = tenantCatalog.findBootstrapById(tenantId)
                    .map(b -> b.timezone())
                    .orElse(ZoneId.of("UTC"));
                var tenantToday = LocalDate.now(zone);

                var targets = autoSessionTargetReader.findOpenSessionsBeforeBusinessDate(
                    tenantId, tenantToday, now, CLOSE_REASON);

                if (!targets.isEmpty()) {
                    log.info("session.auto-close tenant={} tenantToday={} eligible={}",
                        tenantId.value(), tenantToday, targets.size());
                }

                totalTargets += targets.size();
                totalClosed  += autoCloser.closeTargets(targets, now);
            } finally {
                jobContextBinder.clear();
            }
        }

        return new CloseDueSalesSessionsResult(totalTargets, totalClosed, now);
    }
}
