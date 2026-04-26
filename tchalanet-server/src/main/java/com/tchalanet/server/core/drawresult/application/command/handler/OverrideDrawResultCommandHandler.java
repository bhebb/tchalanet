package com.tchalanet.server.core.drawresult.application.command.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.contracts.results.SourceFlags;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.api.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.drawresult.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.drawresult.application.command.model.OverrideDrawResultResult;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.application.service.ResultSlotTimes;
import com.tchalanet.server.core.drawresult.domain.exception.DrawResultOverrideForbiddenException;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.domain.model.DrawSource;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OverrideDrawResultCommandHandler
    implements CommandHandler<OverrideDrawResultCommand, OverrideDrawResultResult> {

  private final ResultSlotCatalog slotReader;
  private final DrawResultWriterPort writer;
  private final DrawResultReaderPort resultReader;
  private final DrawReaderPort drawReader;
  private final DrawLookupPort drawLookup;
  private final DrawLifecyclePort drawWriter;
  private final DomainEventPublisher publisher;
  private final IdGenerator idGenerator;
  private final JsonUtils jsonUtils;

  @Override
  public OverrideDrawResultResult handle(OverrideDrawResultCommand command) {
    String slotKey = normalizeSlotKey(command.slotKey());

    var slot =
        slotReader
            .findByKey(slotKey)
            .orElseThrow(() -> new IllegalArgumentException("result_slot not found: " + slotKey));

    Instant occurredAt =
        ResultSlotTimes.occurredAt(slot.timezone(), command.drawDate(), slot.drawTime());

    // [Règle Override refusé post-SETTLED]
    // 1. Trouver le résultat actuel s'il existe
    resultReader.findByResultSlotIdAndOccurredAt(slot.id(), occurredAt).ifPresent(drId -> {
      if (drawReader.existsSettledDrawForResult(drId)) {
        throw new DrawResultOverrideForbiddenException(drId);
      }
    });

    // source_result (canonique, même pour un override)
    ObjectNode sourceResult = jsonUtils.emptyObjectNode();
    sourceResult.put("mode", "OVERRIDE");
    sourceResult.put("actor", "OPS");
    sourceResult.put("slot_key", slot.slotKey());
    sourceResult.put("provider", slot.provider());
    sourceResult.put("draw_date", command.drawDate().toString());
    sourceResult.put("occurred_at", occurredAt.toString());
    if (command.pick3() != null && !command.pick3().isBlank())
      sourceResult.put("pick3", command.pick3().trim());
    if (command.pick4() != null && !command.pick4().isBlank())
      sourceResult.put("pick4", command.pick4().trim());
    if (command.reason() != null && !command.reason().isBlank())
      sourceResult.put("reason", command.reason().trim());

    // flags
    SourceFlags flagsObj = SourceFlags.manual("OVERRIDE", "OPS");
    var flags = jsonUtils.valueToTree(flagsObj);

    String status = DrawResultStatus.FINAL.name();
    String source = DrawSource.ADMIN_OVERRIDE.name();
    String quality = ResultQuality.COMPLETE.name();

    var res =
        writer.upsert(
            slot.id(),
            occurredAt,
            sourceResult,
            null,
            sourceResult,
            status,
            source,
            flags,
            quality,
            null,
            command.reason(),
            command.force());

    // [Re-apply après override valide]
    // Mettre à jour tous les draws (non SETTLED) liés à cet instant de tirage
    var draws = drawReader.findByDrawResultId(res.id());
    for (var summary : draws) {
      if (summary.status() != DrawStatus.SETTLED) {
        var draw = drawLookup.findById(summary.id()).orElseThrow();
        draw.applyResult(res.id(), Instant.now(), DrawSource.ADMIN_OVERRIDE);
        drawWriter.save(draw);

        // Publication de l'événement de re-apply
        var event = new DrawResultAppliedEvent(
            EventId.of(idGenerator.newUuid()),
            Instant.now(),
            draw.tenantId(),
            draw.id(),
            slot.id(),
            res.id()
        );
        AfterCommit.run(() -> publisher.publish(event));
      }
    }

    log.info(
        "draw_result.override slotKey={} occurredAt={} drawResultId={} created={} updated={}",
        slot.slotKey(),
        occurredAt,
        res.id(),
        res.created(),
        res.updated());

    return new OverrideDrawResultResult(res.id(), res.created(), res.updated());
  }

  private static String normalizeSlotKey(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }
}
