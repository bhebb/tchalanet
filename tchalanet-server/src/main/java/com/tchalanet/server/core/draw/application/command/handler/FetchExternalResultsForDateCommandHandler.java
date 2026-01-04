package com.tchalanet.server.core.draw.application.command.handler;

import static com.tchalanet.server.core.draw.domain.model.DrawResultUpsertResult.*;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.FetchExternalResultsForDateCommand;
import com.tchalanet.server.core.draw.application.command.model.FetchExternalResultsForDateResult;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class FetchExternalResultsForDateCommandHandler
    implements CommandHandler<
        FetchExternalResultsForDateCommand, FetchExternalResultsForDateResult> {

  private final ExternalDrawResultPort externalPort;
  private final DrawResultWriterPort drawResultStore;

  @Override
  public FetchExternalResultsForDateResult handle(FetchExternalResultsForDateCommand cmd) {
    validate(cmd);

    var normalized =
        cmd.channelCodes().stream()
            .filter(Objects::nonNull)
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .filter(s -> !s.isBlank())
            .distinct()
            .limit(cmd.maxDraws()) // hard limit safety
            .toList();

    if (normalized.isEmpty()) {
      return new FetchExternalResultsForDateResult(0, 0, 0, 0, 0);
    }

    var bulkQuery =
        new ExternalDrawResultPort.DrawExternalBulkQuery(
            normalized,
            cmd.drawDate(),
            0, // daysBack (ici: date précise)
            cmd.force(),
            cmd.dryRun());

    var results = externalPort.fetchExternalResults(bulkQuery);
    if (results == null) results = java.util.Map.of();

    int inserted = 0, updated = 0, noop = 0, skipped = 0, notFound = 0;

    // itère sur la liste demandée pour compter aussi les absents
    for (String channelCode : normalized) {
      var ext = results.get(channelCode);

      // absent ou non exploitable => notFound
      if (ext == null || !ext.found() || ext.numbers() == null || ext.numbers().isEmpty()) {
        notFound++;
        continue;
      }

      if (cmd.dryRun()) {
        // en dry-run, on pourrait compter "wouldInsert/wouldUpdate" mais on n'a pas l'info
        // => on ne touche pas aux compteurs
        continue;
      }

      var decision =
          drawResultStore.upsertFromExternal(channelCode, cmd.drawDate(), ext, cmd.force());

      switch (decision) {
        case INSERTED -> inserted++;
        case UPDATED -> updated++;
        case NOOP -> noop++;
        case SKIPPED -> skipped++;
      }
    }

    log.info(
        "fetchExternalResultsForDate: date={} channels={} inserted={} updated={} noop={} skipped={} notFound={} force={} dryRun={}",
        cmd.drawDate(),
        normalized.size(),
        inserted,
        updated,
        noop,
        skipped,
        notFound,
        cmd.force(),
        cmd.dryRun());

    return new FetchExternalResultsForDateResult(inserted, updated, noop, skipped, notFound);
  }

  private static void validate(FetchExternalResultsForDateCommand cmd) {
    Objects.requireNonNull(cmd, "command is required");
    Objects.requireNonNull(cmd.drawDate(), "drawDate is required");
    if (cmd.channelCodes() == null || cmd.channelCodes().isEmpty()) {
      throw new IllegalArgumentException("channelCodes is required");
    }
    if (cmd.maxDraws() <= 0) throw new IllegalArgumentException("maxDraws must be > 0");
  }
}
