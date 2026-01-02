package com.tchalanet.server.core.draw.infra.batch.results.fetch;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class FetchExternalResultProcessor implements ItemProcessor<DrawId, ApplyResultRow> {

  private final DrawReaderPort drawReaderPort;
  private final ExternalDrawResultPort externalDrawResultPort;
  private final ExternalResultsSlotCache slotCache;

  private final JobParameters jp;

  public FetchExternalResultProcessor(
      DrawReaderPort drawReaderPort,
      ExternalDrawResultPort externalDrawResultPort,
      ExternalResultsSlotCache slotCache,
      @Value("#{stepExecution.jobExecution.jobParameters}") JobParameters jp) {
    this.drawReaderPort = drawReaderPort;
    this.externalDrawResultPort = externalDrawResultPort;
    this.slotCache = slotCache;
    this.jp = jp;
  }

  private List<String> parseCsvChannels() {
    var csv = jp.getString("channel_codes"); // ex: "US_FL_NUM3_MID,US_FL_NUM3_EVE"
    if (csv == null || csv.isBlank()) return List.of();

    return Arrays.stream(csv.split(","))
        .map(s -> s == null ? "" : s.trim().toUpperCase())
        .filter(s -> !s.isBlank())
        .distinct()
        .toList();
  }

  @Override
  public ApplyResultRow process(DrawId drawId) {
    var drawOpt = drawReaderPort.findById(drawId);
    if (drawOpt.isEmpty()) return null;
    var draw = drawOpt.get();

    boolean force = "true".equalsIgnoreCase(jp.getString("force"));
    boolean dryRun = "true".equalsIgnoreCase(jp.getString("dry_run"));

    Long maxDrawsL = jp.getLong("max_draws");
    int maxDraws = maxDrawsL == null ? 200 : maxDrawsL.intValue();

    var drawDateStr = jp.getString("draw_date");
    if (drawDateStr == null || drawDateStr.isBlank()) return null;
    var drawDate = LocalDate.parse(drawDateStr);

    var channelCodes = parseCsvChannels();

    // 1) Bulk fetch ONCE per step execution
    if (!slotCache.isLoaded()) {
      var query =
          new ExternalDrawResultPort.DrawExternalBulkQuery(
              channelCodes, drawDate, maxDraws, force, dryRun);
      var bulk = externalDrawResultPort.fetchExternalResults(query);
      if (bulk != null) slotCache.putAll(bulk);
      slotCache.markLoaded();
    }

    // 2) Pick for current draw
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
