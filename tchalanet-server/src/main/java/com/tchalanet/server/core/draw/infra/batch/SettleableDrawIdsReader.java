package com.tchalanet.server.core.draw.infra.batch;

import com.tchalanet.server.core.draw.application.port.out.FindSettleableDrawIdsPort;
import com.tchalanet.server.core.draw.application.port.out.FindSettleableDrawIdsPort.SettleableDrawCriteria;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
 *
 * <p>Note: désormais paramétrable via JobParameters (tenant_id, source, provider, channel_code,
 * days_back, max_draws, force). Le port accepte un critère complet.
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
    JobCtx ctx = JobCtx.from(jobParameters, clock);
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
      String tenantIdStr = jp.getString("tenant_id");
      UUID tenantId = tenantIdStr != null && !tenantIdStr.isBlank() ? UUID.fromString(tenantIdStr) : null;
      String source = jp.getString("source");
      String provider = jp.getString("provider");
      String channelCode = jp.getString("channel_code");
      Long daysBack = jp.getLong("days_back");
      Long maxDraws = jp.getLong("max_draws");
      boolean force = "true".equalsIgnoreCase(jp.getString("force"));

      Instant now = Instant.now(clock);
      Instant from = daysBack != null ? now.minusSeconds(daysBack * 86400) : now.minusSeconds(6L * 86400);
      Instant to = now;

      return new JobCtx(tenantId, source, provider, channelCode, from, to, maxDraws, force);
    }
  }
}
