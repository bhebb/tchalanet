package com.tchalanet.server.core.draw.internal.infra.batch.results.settle;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.internal.application.port.out.FindSettleableDrawIdsPort;
import com.tchalanet.server.core.draw.internal.application.port.out.FindSettleableDrawIdsPort.SettleableDrawCriteria;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@StepScope
public class SettleableDrawIdsReader extends IteratorItemReader<DrawId> {


    public SettleableDrawIdsReader(
        FindSettleableDrawIdsPort port,
        Clock clock,
        @Value("#{jobParameters['tenant_id']}") String tenantId,
        @Value("#{jobParameters['days_back']}") Long daysBack,
        @Value("#{jobParameters['max_draws']}") Long maxDraws,
        @Value("#{jobParameters['force']}") String force
    ) {
        super(fetch(port, clock, tenantId, daysBack, maxDraws, force));
    }

    private static List<DrawId> fetch(
        FindSettleableDrawIdsPort port,
        Clock clock,
        String tenantId,
        Long daysBack,
        Long maxDraws,
        String force
    ) {
        var ctx = JobCtx.from(clock, tenantId, daysBack, maxDraws, force);

        var criteria = new SettleableDrawCriteria(
            ctx.tenantId,
            ctx.from,
            ctx.to,
            ctx.maxDraws != null ? ctx.maxDraws : 1000L,
            ctx.force
        );

        return port.findSettleableDrawIds(criteria);
    }

    private record JobCtx(
        TenantId tenantId,
        Instant from,
        Instant to,
        Long maxDraws,
        boolean force
    ) {
        static JobCtx from(
            Clock clock,
            String tenantIdRaw,
            Long daysBack,
            Long maxDraws,
            String forceRaw
        ) {
            var tenantIdStr = trimToNull(tenantIdRaw);
            if (tenantIdStr == null) {
                throw new IllegalArgumentException("tenant_id is required (job parameter)");
            }
            var tenantId = TenantId.parse(tenantIdStr);

            if (daysBack != null && daysBack < 0) {
                throw new IllegalArgumentException("days_back must be >= 0");
            }

            if (maxDraws != null && maxDraws <= 0) {
                throw new IllegalArgumentException("max_draws must be > 0");
            }

            var forceValue = trimToNull(forceRaw);
            boolean force = "true".equalsIgnoreCase(forceValue != null ? forceValue : "false");

            var now = Instant.now(clock);
            long backDays = (daysBack != null ? daysBack : 6L);

            var from = now.minusSeconds(backDays * 86_400L);
            var to = now;

            return new JobCtx(tenantId, from, to, maxDraws, force);
        }

        private static String trimToNull(String value) {
            if (value == null) return null;
            var trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

    }
}
