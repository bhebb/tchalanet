package com.tchalanet.server.core.drawresult.application.command.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.contracts.haiti.HaitiFlags;
import com.tchalanet.server.common.contracts.haiti.HaitiProjectionOutput;
import com.tchalanet.server.common.contracts.results.ExternalResultOutput;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.DateWindows;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowResult;
import com.tchalanet.server.core.drawresult.application.port.out.ExternalResultsFetchPort;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.internal.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.drawresult.internal.util.SourceResultBuilder;
import com.tchalanet.server.core.haiti.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class FetchExternalResultsWindowCommandHandler
    implements CommandHandler<FetchExternalResultsWindowCommand, FetchExternalResultsWindowResult> {

  private final ExternalResultsFetchPort fetchPort;
  private final HaitiLotteryPort haitiPort;
  private final HaitiProjectionConfigPort haitiConfigPort;
  private final DrawResultWriterPort writer;

  /** DB is source of truth at runtime (slotKey -> result_slot). */
  private final ResultSlotCatalog resultSlotReader;

  private final DrawResultsProperties props;
  private final ObjectMapper mapper;
  private final Clock clock;

  @Override
  public FetchExternalResultsWindowResult handle(FetchExternalResultsWindowCommand cmd) {
    validate(cmd);

    int inserted = 0, updated = 0, skipped = 0, notFound = 0, errors = 0;

    int daysBack = clampDaysBack(cmd.daysBack());
    List<LocalDate> dates = DateWindows.datesBackInclusive(cmd.baseDate(), daysBack);

    // MVP: default projection config (slot overrides can be layered later)
    var projCfg = haitiConfigPort.getDefault();

    for (LocalDate date : dates) {
      for (String slotKeyRaw : cmd.slotKeys()) {
        final String slotKey = normalizeKey(slotKeyRaw);

        try {
          var slotOpt = resultSlotReader.findBySlotKey(slotKey);
          if (slotOpt.isEmpty()) {
            notFound++;
            continue;
          }

          var slot = slotOpt.get();
          if (!slot.active()) {
            notFound++;
            continue;
          }

          // Slot-first fetch
          // NOTE: if you want maxSlots enforced end-to-end, extend ResultSlotFetchQuery to include
          // it.
          var q =
              new ExternalResultsFetchPort.ResultSlotFetchQuery(
                  slot.slotKey(), date, cmd.force(), cmd.dryRun(), Instant.now(clock));
          var bundle = fetchPort.fetchSlot(q);

          ExternalResultOutput p3 = bundle == null ? null : bundle.pick3();
          ExternalResultOutput p4 = bundle == null ? null : bundle.pick4();

          boolean anyFound =
              (p3 != null && p3.found() && !p3.main().isEmpty())
                  || (p4 != null && p4.found() && !p4.main().isEmpty());

          if (!anyFound) {
            notFound++;
            continue;
          }

          if (cmd.dryRun()) {
            skipped++;
            continue;
          }

          Instant occurredAt =
              OccurredAtResolver.resolve(
                  // prefer provider occurredAt if available
                  (p3 != null ? p3.occurredAt() : (p4 != null ? p4.occurredAt() : null)),
                  date,
                  slot.drawTime(),
                  slot.timezone(),
                  clock);

          var sourceResult =
              SourceResultBuilder.build(
                  mapper, slot.provider(), slot.slotKey(), date, occurredAt, p3, p4);

          // Haiti projection: MUST ALWAYS produce lot1..lot4 (DB constraints)
          ObjectNode haitiResultNode;
          HaitiFlags haitiFlags;

          try {
            String pick3 =
                (p3 != null && p3.found() && !p3.main().isEmpty())
                    ? String.join("", p3.main())
                    : "";
            String pick4 =
                (p4 != null && p4.found() && !p4.main().isEmpty())
                    ? String.join("", p4.main())
                    : "";

            var pick =
                new com.tchalanet.server.core.haiti.domain.lottery.model.ExternalPick(pick3, pick4);
            HaitiProjectionOutput hp = haitiPort.projectResult(pick, projCfg);

            // ensure not-null + required keys
            haitiResultNode = coerceHaitiLots(hp == null ? null : hp.result());
            haitiFlags = hp == null ? HaitiFlags.fail(1, "PROJECTION_NULL", Map.of()) : hp.flags();

          } catch (Exception e) {
            log.warn(
                "draw-results.fetch projection_failed slot={} date={} err={}",
                slot.slotKey(),
                date,
                e.toString());
            haitiResultNode = emptyHaitiLots();
            haitiFlags =
                HaitiFlags.fail(
                    1, "PROJECTION_EXCEPTION", Map.of("error", String.valueOf(e.getMessage())));
          }

          // Flags: keep pick3 + pick4 flags separately (more informative)
          var flags = mapper.createObjectNode();
          var sourceFlags = mapper.createObjectNode();
          if (p3 != null) sourceFlags.set("pick3", mapper.valueToTree(p3.sourceFlags()));
          if (p4 != null) sourceFlags.set("pick4", mapper.valueToTree(p4.sourceFlags()));
          flags.set("source", sourceFlags);
          flags.set("haiti", mapper.valueToTree(haitiFlags));

          // Raw payload: keep both + optional bundle raw
          var raw = mapper.createObjectNode();
          if (p3 != null) raw.set("pick3_raw", mapper.valueToTree(p3.rawPayload()));
          if (p4 != null) raw.set("pick4_raw", mapper.valueToTree(p4.rawPayload()));
          if (bundle != null && bundle.raw() != null)
            raw.set("provider_payload", mapper.valueToTree(bundle.raw()));

          var chosenQuality =
              (p3 != null && p3.found())
                  ? p3.quality()
                  : (p4 != null && p4.found() ? p4.quality() : null);
          String quality = chosenQuality == null ? null : chosenQuality.name();

          String sourceHash =
              (p3 != null && p3.found())
                  ? p3.sourceFlags().hash()
                  : (p4 != null && p4.found() ? p4.sourceFlags().hash() : null);

          var up =
              writer.upsert(
                  slot.id(),
                  occurredAt,
                  sourceResult,
                  haitiResultNode, // ALWAYS non-null with lot1..lot4
                  raw,
                  "PROVISIONAL",
                  "EXTERNAL",
                  flags,
                  quality,
                  sourceHash,
                  null,
                  cmd.force());

          if (up == null || up.id() == null) {
            skipped++;
            continue;
          }

          if (up.created()) inserted++;
          else if (up.updated()) updated++;
          else skipped++;

        } catch (Exception e) {
          errors++;
          log.warn("draw-results.fetch failed slot={} date={} err={}", slotKey, date, e.toString());
        }
      }
    }

    return new FetchExternalResultsWindowResult(inserted, updated, errors, skipped, notFound);
  }

  private int clampDaysBack(int v) {
    int x = Math.max(0, v);
    return Math.min(x, props.getLimits().getHardDaysBack());
  }

  private static void validate(FetchExternalResultsWindowCommand cmd) {
    Objects.requireNonNull(cmd, "command is required");
    Objects.requireNonNull(cmd.baseDate(), "baseDate is required");
    if (cmd.daysBack() < 0) throw new IllegalArgumentException("daysBack must be >= 0");
    if (cmd.slotKeys() == null || cmd.slotKeys().isEmpty())
      throw new IllegalArgumentException("slotKeys required");
    if (cmd.maxSlots() <= 0) throw new IllegalArgumentException("maxSlots must be > 0");
  }

  private static String normalizeKey(String s) {
    return s == null ? "" : s.trim().toUpperCase(java.util.Locale.ROOT);
  }

  /** Ensure haiti_result satisfies DB constraints (lot1..lot4 must exist). */
  private ObjectNode coerceHaitiLots(Object anyResult) {
    if (anyResult == null) return emptyHaitiLots();
    // Accept Map or POJO -> JsonNode
    var node = mapper.valueToTree(anyResult);
    if (node == null || node.isNull() || !node.isObject()) return emptyHaitiLots();

    var obj = (ObjectNode) node;
    // enforce required keys
    if (!obj.has("lot1")) obj.put("lot1", "");
    if (!obj.has("lot2")) obj.put("lot2", "");
    if (!obj.has("lot3")) obj.put("lot3", "");
    if (!obj.has("lot4")) obj.put("lot4", "");
    return obj;
  }

  private ObjectNode emptyHaitiLots() {
    var o = mapper.createObjectNode();
    o.put("lot1", "");
    o.put("lot2", "");
    o.put("lot3", "");
    o.put("lot4", "");
    return o;
  }
}
