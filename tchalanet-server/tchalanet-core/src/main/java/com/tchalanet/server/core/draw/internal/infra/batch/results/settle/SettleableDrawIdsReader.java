package com.tchalanet.server.core.draw.internal.infra.batch.results.settle;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.core.draw.internal.application.port.out.FindSettleableDrawIdsPort;
import com.tchalanet.server.core.draw.internal.application.port.out.FindSettleableDrawIdsPort.SettleableDrawCriteria;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.parameters.JobParameters;
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
        @Value("#{jobParameters}") JobParameters jobParameters
    ) {
        super(fetch(port, clock, jobParameters));
    }

    private static List<DrawId> fetch(
        FindSettleableDrawIdsPort port,
        Clock clock,
        JobParameters jobParameters
    ) {
        var ctx = JobCtx.from(jobParameters, clock);

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
        String source,
        String provider,
        String channelCode,
        Instant from,
        Instant to,
        Long maxDraws,
        boolean force
    ) {
        static JobCtx from(JobParameters jp, Clock clock) {
            var tenantIdStr = trimToNull(jp.getString(JobParamKeys.TENANT_ID));
            if (tenantIdStr == null) {
                throw new IllegalArgumentException("tenant_id is required (job parameter)");
            }
            var tenantId = TenantId.parse(tenantIdStr);

            // optional, but avoid null surprises
            var source = defaultStr(trimToNull(jp.getString(JobParamKeys.SOURCE)), "batch");

            // provider is expected for this settle pipeline
            var provider = trimToNull(jp.getString(JobParamKeys.PROVIDER));
            if (provider == null) {
                throw new IllegalArgumentException("provider is required (job parameter)");
            }

            var channelCode = trimToNull(jp.getString(JobParamKeys.CHANNEL_CODE));

            var daysBack = jp.getLong(JobParamKeys.DAYS_BACK);
            if (daysBack != null && daysBack < 0) {
                throw new IllegalArgumentException("days_back must be >= 0");
            }

            var maxDraws = jp.getLong(JobParamKeys.MAX_DRAWS);
            if (maxDraws != null && maxDraws <= 0) {
                throw new IllegalArgumentException("max_draws must be > 0");
            }

            boolean force = "true".equalsIgnoreCase(defaultStr(trimToNull(jp.getString("force")), "false"));

            var now = Instant.now(clock);
            long backDays = (daysBack != null ? daysBack : 6L);

            var from = now.minusSeconds(backDays * 86_400L);
            var to = now;

            return new JobCtx(tenantId, source, provider, channelCode, from, to, maxDraws, force);
        }

        private static String trimToNull(String value) {
            if (value == null) return null;
            var trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        private static String defaultStr(String value, String def) {
            return (value == null) ? def : value;
        }
    }
}
