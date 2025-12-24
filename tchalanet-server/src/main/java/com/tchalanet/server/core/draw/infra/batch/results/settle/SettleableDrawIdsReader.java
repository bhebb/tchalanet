package com.tchalanet.server.core.draw.infra.batch.results.settle;

import com.tchalanet.server.core.draw.application.port.out.FindSettleableDrawIdsPort;
import com.tchalanet.server.core.draw.application.port.out.FindSettleableDrawIdsPort.SettleableDrawCriteria;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.infrastructure.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reader qui renvoie les IDs de tirages RESULTED & PENDING dont le cutoff / settlement_due_at est
 * passé.
 */
@Component
@StepScope
public class SettleableDrawIdsReader extends IteratorItemReader<UUID> {

    public SettleableDrawIdsReader(
        FindSettleableDrawIdsPort port,
        Clock clock,
        @Value("#{jobParameters}") JobParameters jobParameters) {
        super(fetch(port, clock, jobParameters));
    }

    private static List<UUID> fetch(
        FindSettleableDrawIdsPort port, Clock clock, JobParameters jobParameters) {
        var ctx = JobCtx.from(jobParameters, clock);
        var criteria =
            new SettleableDrawCriteria(
                ctx.tenantId,
                ctx.source,
                ctx.provider,
                ctx.channelCode,
                ctx.from,
                ctx.to,
                ctx.maxDraws,
                ctx.force);
        return port.findSettleableDrawIds(criteria);
    }

    private record JobCtx(
        UUID tenantId,
        String source,
        String provider,
        String channelCode,
        Instant from,
        Instant to,
        Long maxDraws,
        boolean force) {

        static JobCtx from(JobParameters jp, Clock clock) {
            var tenantIdStr = jp.getString("tenant_id");
            var tenantId = tenantIdStr != null && !tenantIdStr.isBlank() ? UUID.fromString(tenantIdStr) : null;
            var source = jp.getString("source");
            var provider = jp.getString("provider");
            var channelCode = jp.getString("channel_code");
            var daysBack = jp.getLong("days_back");
            var maxDraws = jp.getLong("max_draws");
            var force = "true".equalsIgnoreCase(jp.getString("force"));

            var now = Instant.now(clock);
            var from = daysBack != null ? now.minusSeconds(daysBack * 86400) : now.minusSeconds(6L * 86400);
            var to = now;

            return new JobCtx(tenantId, source, provider, channelCode, from, to, maxDraws, force);
        }
    }
}
