package com.tchalanet.server.core.draw.infra.batch.results.fetch;

import com.tchalanet.server.core.draw.infra.persistence.repo.DrawBatchQueryRepository;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawChannelJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.infrastructure.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reader SLOT: ids CLOSED pour (tenant_id, channel_code, draw_date_local). */
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

    var now = Instant.now(clock);

    var tenantIdStr = jp.getString("tenant_id");
    if (tenantIdStr == null || tenantIdStr.isBlank()) {
      throw new IllegalArgumentException("tenant_id required");
    }
    var tenantId = UUID.fromString(tenantIdStr);

    var channelCode = jp.getString("channel_code");
    if (channelCode == null || channelCode.isBlank()) {
      throw new IllegalArgumentException("channel_code required");
    }

    var force = "true".equalsIgnoreCase(jp.getString("force"));

    var maxDrawsParam = jp.getLong("max_draws");
    var maxDraws = maxDrawsParam != null ? maxDrawsParam.intValue() : 200;
    maxDraws = clamp(maxDraws, 1, 5000);

    var drawDateStr = jp.getString("draw_date");

    var zone =
        drawChannelJpaRepository
            .findByTenantIdAndCode(tenantId, channelCode)
            .map(ch -> ZoneId.of(ch.getTimezone().trim()))
            .orElse(ZoneId.of("UTC"));

    var drawDateLocal =
        Optional.ofNullable(drawDateStr)
            .filter(s -> !s.isBlank())
            .map(LocalDate::parse)
            .orElseGet(() -> ZonedDateTime.ofInstant(now, zone).toLocalDate());

    var dayStartUtc = ZonedDateTime.of(drawDateLocal.atStartOfDay(), zone).toInstant();
    var dayEndUtc = ZonedDateTime.of(drawDateLocal.plusDays(1).atStartOfDay(), zone).toInstant();
    var eligibleBeforeUtc = now.minusSeconds(5 * 60L);

    return drawBatchQueryRepository.findClosedDrawIdsForSlot(
        tenantId, channelCode, dayStartUtc, dayEndUtc, eligibleBeforeUtc, force, maxDraws);
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(value, max));
  }
}
