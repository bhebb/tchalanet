package com.tchalanet.server.core.draw.infra.batch;

import com.tchalanet.server.core.draw.infra.persistence.DrawBatchQueryRepository;
import com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.infrastructure.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reader SLOT: renvoie les drawIds CLOSED (eligible<=now-5min) pour (tenant_id, channel_code, draw_date_local). */
@Component
@StepScope
public class FetchableDrawIdsReader extends IteratorItemReader<UUID> {

  public FetchableDrawIdsReader(
      DrawBatchQueryRepository drawBatchQueryRepository,
      DrawChannelJpaRepository drawChannelJpaRepository,
      Clock clock,
      @Value("#{jobParameters}") JobParameters jp) {
    super(fetch(drawBatchQueryRepository, drawChannelJpaRepository, clock, jp));
  }

  private static List<UUID> fetch(
      DrawBatchQueryRepository drawBatchQueryRepository,
      DrawChannelJpaRepository drawChannelJpaRepository,
      Clock clock,
      JobParameters jp) {

    Instant now = Instant.now(clock);

    String tenantIdStr = jp.getString("tenant_id");
    if (tenantIdStr == null || tenantIdStr.isBlank()) {
      throw new IllegalArgumentException("tenant_id required");
    }
    UUID tenantId = UUID.fromString(tenantIdStr);

    String channelCode = jp.getString("channel_code");
    if (channelCode == null || channelCode.isBlank()) {
      throw new IllegalArgumentException("channel_code required");
    }

    boolean force = "true".equalsIgnoreCase(jp.getString("force"));

    Long maxDrawsParam = jp.getLong("max_draws");
    int maxDraws = maxDrawsParam != null ? maxDrawsParam.intValue() : 200;
    maxDraws = Math.clamp(maxDraws, 1, 5000);

    String drawDateStr = jp.getString("draw_date");

    ZoneId zone = ZoneId.of("UTC");
    var channel = drawChannelJpaRepository.findByTenantIdAndCode(tenantId, channelCode).orElse(null);
    if (channel != null && channel.getTimezone() != null && !channel.getTimezone().isBlank()) {
      zone = ZoneId.of(channel.getTimezone().trim());
    }

    LocalDate drawDateLocal =
        (drawDateStr == null || drawDateStr.isBlank())
            ? ZonedDateTime.ofInstant(now, zone).toLocalDate()
            : LocalDate.parse(drawDateStr);

    Instant dayStartUtc = ZonedDateTime.of(drawDateLocal.atStartOfDay(), zone).toInstant();
    Instant dayEndUtc = ZonedDateTime.of(drawDateLocal.plusDays(1).atStartOfDay(), zone).toInstant();
    Instant eligibleBeforeUtc = now.minusSeconds(5 * 60L);

    return drawBatchQueryRepository.findClosedDrawIdsForSlot(
        tenantId, channelCode, dayStartUtc, dayEndUtc, eligibleBeforeUtc, force, maxDraws);
  }
}
