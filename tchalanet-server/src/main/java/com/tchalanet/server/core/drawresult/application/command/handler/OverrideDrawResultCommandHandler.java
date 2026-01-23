package com.tchalanet.server.catalog.drawresult.application.command.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.contracts.results.SourceFlags;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.catalog.drawresult.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.catalog.drawresult.application.command.model.OverrideDrawResultResult;
import com.tchalanet.server.catalog.drawresult.application.service.ResultSlotTimes;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.catalog.drawresult.domain.model.DrawSource;
import com.tchalanet.server.catalog.drawresult.internal.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
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
  private final ObjectMapper mapper;

  @Override
  public OverrideDrawResultResult handle(OverrideDrawResultCommand command) {
    String slotKey = normalizeSlotKey(command.slotKey());

    var slot =
        slotReader
            .findBySlotKey(slotKey)
            .orElseThrow(() -> new IllegalArgumentException("result_slot not found: " + slotKey));

    Instant occurredAt =
        ResultSlotTimes.occurredAt(slot.timezone(), command.drawDate(), slot.drawTime());

    // source_result (canonique, même pour un override)
    ObjectNode sourceResult = mapper.createObjectNode();
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
    var flags = mapper.valueToTree(flagsObj);

    // Status / Source / Quality : on évite les strings libres
    String status = DrawResultStatus.FINAL.name(); // adapte si ton enum diffère
    String source = DrawSource.ADMIN_OVERRIDE.name(); // ou MANUAL si tu préfères
    String quality = ResultQuality.COMPLETE.name();

    var res =
        writer.upsert(
            slot.id(),
            occurredAt,
            sourceResult, // sourceResult (US normalized)
            null, // haitiResult: optionnel ici (writer met lot1..lot4 null)
            sourceResult, // rawPayload: tu peux mettre null si tu veux
            status,
            source,
            flags,
            quality,
            null, // sourceHash (optionnel)
            command.reason(), // overrideReason
            command.force());

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
