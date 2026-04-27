package com.tchalanet.server.core.drawresult.application.command.handler;

import tools.jackson.databind.node.ObjectNode;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.contracts.results.SourceFlags;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.core.draw.api.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.drawresult.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.drawresult.application.command.model.OverrideDrawResultResult;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.domain.exception.DrawResultOverrideForbiddenException;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import java.time.Clock;
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
  private final Clock clock;

  @Override
  public OverrideDrawResultResult handle(OverrideDrawResultCommand command) {
    String slotKey = normalizeSlotKey(command.slotKey());

    var slot =
        slotReader
            .findByKey(slotKey)
            .orElseThrow(() -> new IllegalArgumentException("result_slot not found: " + slotKey));

    // [D7] Use OccurredAtResolver instead of ResultSlotTimes
    Instant occurredAt = OccurredAtResolver.resolve(
        null, command.drawDate(), slot.drawTime(), slot.timezone(), clock);

    // [Règle Override refusé post-SETTLED]
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

    // [D10] Use enum.name()
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
    var draws = drawReader.findByDrawResultId(res.id());
    for (var summary : draws) {
      if (summary.status() != DrawStatus.SETTLED) {
        var draw = drawLookup.findById(summary.id()).orElseThrow();
        draw.applyResult(res.id(), Instant.now(clock), DrawSource.ADMIN_OVERRIDE);
        drawWriter.save(draw);

        var event = new DrawResultAppliedEvent(
            EventId.of(idGenerator.newUuid()),
            Instant.now(clock),
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
