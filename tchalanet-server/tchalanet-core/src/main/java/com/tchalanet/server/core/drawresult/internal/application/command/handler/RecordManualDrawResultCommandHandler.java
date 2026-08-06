package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultResult;
import com.tchalanet.server.core.drawresult.api.error.DrawResultErrorCodes;
import com.tchalanet.server.core.drawresult.api.event.GlobalDrawResultAvailableEvent;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.api.model.ResultSource;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalSourceFlags;
import com.tchalanet.server.core.haiti.api.HaitiProjectionOutput;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.haiti.internal.domain.lottery.exception.InvalidExternalPickException;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ObjectNode;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordManualDrawResultCommandHandler
    implements CommandHandler<RecordManualDrawResultCommand, RecordManualDrawResultResult> {

  private final ResultSlotCatalog slotReader;
  private final DrawResultWriterPort writer;
  private final HaitiLotteryPort haitiLotteryPort;
  private final JsonUtils jsonUtils;
  private final HaitiProjectionConfigPort haitiProjectionConfigPort;
  private final DomainEventPublisher eventPublisher;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public RecordManualDrawResultResult handle(RecordManualDrawResultCommand command) {
    TenantId tenantId = command.tenantId();
    var slot = resolveSlot(command.slotKey());
    var occurredAt = resolveOccurredAt(command, slot);
    var sourceResult = buildSourceResult(command, slot, occurredAt);
    var flags = buildFlags(command);
    var haitiResult = projectHaiti(command, slot);

    var status =
        command.observeTrustPolicy() ? DrawResultStatus.PROVISIONAL : DrawResultStatus.CONFIRMED;

    var res =
        writer.upsert(
            slot.id(),
            command.drawDate(),
            occurredAt,
            sourceResult,
            jsonUtils.toJsonNode(haitiResult.result()),
            sourceResult,
            status.name(),
            DrawSource.MANUAL.name(),
            flags,
            ResultQuality.COMPLETE.name(),
            null,
            command.reason(),
            command.force());

    log.info(
        "draw_result.manual tenant={} slotKey={} occurredAt={} resultId={} created={} updated={}",
        tenantId,
        slot.slotKey(),
        occurredAt,
        res.id(),
        res.created(),
        res.updated());

    if (res.created()) {
      var event =
          new GlobalDrawResultAvailableEvent(
              EventId.of(idGenerator.newUuid()),
              Instant.now(),
              null,
              slot.id(),
              slot.slotKey(),
              res.id(),
              occurredAt,
              command.drawDate(),
              slot.provider(),
              ResultSource.MANUAL_ENTRY);
      // The listener is a transactional event listener. Publish while this use-case
      // transaction is active so Spring can dispatch it after the transaction commits.
      eventPublisher.publish(event);
    }

    return new RecordManualDrawResultResult(res.id(), res.created(), res.updated());
  }

  private ResultSlotView resolveSlot(String rawSlotKey) {
    var slotKey = normalizeSlotKey(rawSlotKey);

    return slotReader
        .findByKey(slotKey)
        .orElseThrow(() -> ProblemRest.of(DrawResultErrorCodes.RESULT_SLOT_NOT_FOUND));
  }

  private Instant resolveOccurredAt(RecordManualDrawResultCommand command, ResultSlotView slot) {
    return OccurredAtResolver.resolveOrThrow(
        null, command.drawDate(), slot.drawTime(), slot.timezone());
  }

  private ObjectNode buildSourceResult(
      RecordManualDrawResultCommand command, ResultSlotView slot, Instant occurredAt) {

    var sourceResult = JsonUtils.emptyObject();
    sourceResult.put("mode", "MANUAL");
    sourceResult.put("slot_key", slot.slotKey());
    sourceResult.put("provider", slot.provider());
    sourceResult.put("draw_date", command.drawDate().toString());
    sourceResult.put("occurred_at", occurredAt.toString());
    sourceResult.put("recorded_by", emptyIfNull(command.recordedBy()));
    sourceResult.put("notes", emptyIfNull(command.notes()));
    sourceResult.put("reason", emptyIfNull(command.reason()));

    putIfNotBlank(sourceResult, "pick3", command.pick3());
    putIfNotBlank(sourceResult, "pick4", command.pick4());

    return sourceResult;
  }

  private tools.jackson.databind.JsonNode buildFlags(RecordManualDrawResultCommand command) {
    return jsonUtils.toJsonNode(ExternalSourceFlags.manual(emptyIfNull(command.recordedBy())));
  }

  private HaitiProjectionOutput projectHaiti(
      RecordManualDrawResultCommand command, ResultSlotView slot) {
    try {
      var projection =
          haitiLotteryPort.projectResult(
              ExternalPick.of(command.pick3(), command.pick4()),
              haitiProjectionConfigPort.resolve(slot.projectionCfg()));
      if (!projection.flags().projectionOk() || projection.result() == null) {
        log.error("draw_result.manual projection_failed slotKey={}", slot.slotKey());
        throw ProblemRest.of(DrawResultErrorCodes.PROJECTION_FAILED);
      }
      return projection;
    } catch (InvalidExternalPickException e) {
      throw ProblemRest.of(DrawResultErrorCodes.INVALID_EXTERNAL_PICK, Map.of(), e);
    }
  }

  private static void putIfNotBlank(ObjectNode node, String field, String value) {
    if (value != null && !value.isBlank()) {
      node.put(field, value.trim());
    }
  }

  private static String normalizeSlotKey(String key) {
    return key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
  }

  private static String emptyIfNull(String value) {
    return value == null ? "" : value.trim();
  }
}
