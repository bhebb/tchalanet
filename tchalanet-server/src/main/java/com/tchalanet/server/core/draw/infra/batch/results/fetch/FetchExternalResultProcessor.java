package com.tchalanet.server.core.draw.infra.batch.results.fetch;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class FetchExternalResultProcessor implements ItemProcessor<DrawId, ApplyResultRow> {

  private final DrawReaderPort drawReaderPort;
  private final ExternalDrawResultPort externalDrawResultPort;
  private final ExternalResultsSlotCache slotCache;

  @Value("#{jobParameters}")
  private JobParameters jp;

  private List<String> parseCsvChannels() {
    if (jp == null) return List.of();
    var csv = jp.getString("channel_codes");
    if (csv == null || csv.isBlank()) return List.of();
    return Arrays.stream(csv.split(","))
        .filter(Objects::nonNull)
        .map(s -> s.trim().toUpperCase())
        .filter(s -> !s.isBlank())
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public ApplyResultRow process(DrawId drawId) {
    var drawOpt = drawReaderPort.findById(drawId);
    if (drawOpt.isEmpty()) return null;
    var draw = drawOpt.get();

    if (jp == null) return null; // defensive: job params must be present

    boolean force = "true".equalsIgnoreCase(jp.getString("force"));
    boolean dryRun = "true".equalsIgnoreCase(jp.getString("dry_run"));
    int maxDraws = jp.getLong("max_draws") == null ? 200 : jp.getLong("max_draws").intValue();

    var drawDateStr = jp.getString("draw_date");
    if (drawDateStr == null || drawDateStr.isBlank()) return null; // no date -> nothing to do
    var drawDate = LocalDate.parse(drawDateStr);

    var channelCodes = parseCsvChannels();

    // 1) bulk fetch once / step
    if (!slotCache.isLoaded()) {
      var query = new ExternalDrawResultPort.DrawExternalBulkQuery(channelCodes, drawDate, maxDraws, force, dryRun);
      var bulk = externalDrawResultPort.fetchExternalResults(query);
      if (bulk != null) slotCache.putAll(bulk);
      slotCache.markLoaded();
    }

    // 2) pick result by draw channel
    var res = slotCache.get(draw.drawChannel().code());
    if (res == null || !res.found()) return null;

    return new ApplyResultRow(
        draw.tenantId(),
        draw.id(),
        res.numbers(),
        res.numbersExtra(),
        res.occurredAt(),
        res.rawPayload());
  }
}
