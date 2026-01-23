package com.tchalanet.server.core.uslottery.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.contracts.results.ExternalFetchStatus;
import com.tchalanet.server.common.contracts.results.ExternalResultOutput;
import com.tchalanet.server.common.contracts.results.IngestionMode;
import com.tchalanet.server.common.contracts.results.SourceFlags;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.application.port.out.ExternalResultsFetchPort;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.core.uslottery.application.port.out.ProviderDrawQuery;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
@RequiredArgsConstructor
public class UsLotteryExternalResultsFetchPortAdapter implements ExternalResultsFetchPort {

  private final List<UsLotteryProviderClient> providers;
  private final ResultSlotCatalog resultSlotReader;

  // ----------------------------
  // NEW: slot-first API
  // ----------------------------
  @Override
  public ExternalBundle fetchSlot(ResultSlotFetchQuery q) {
    if (q == null) return new ExternalBundle(null, null, Map.of());

    var slotOpt = resultSlotReader.findBySlotKey(q.slotKey());
    if (slotOpt.isEmpty()) {
      return new ExternalBundle(
          ExternalResultOutput.notFound(
              "SLOT_NOT_FOUND",
              flags(
                  "unknown",
                  ExternalFetchStatus.NOT_FOUND,
                  ResultQuality.SUSPECT,
                  q.now(),
                  Map.of("slotKey", q.slotKey())),
              Map.of("slotKey", q.slotKey())),
          null,
          Map.of("slotKey", q.slotKey()));
    }

    var slot = slotOpt.get();
    String providerKey = safe(slot.provider()); // e.g. "NY", "FL", ...
    var client = findProvider(providerKey);
    if (client == null) {
      return new ExternalBundle(
          ExternalResultOutput.notFound(
              "PROVIDER_NOT_CONFIGURED",
              flags(
                  providerKey,
                  ExternalFetchStatus.ERROR,
                  ResultQuality.SUSPECT,
                  q.now(),
                  Map.of("provider", providerKey)),
              Map.of("provider", providerKey)),
          null,
          Map.of("provider", providerKey));
    }

    // Extract pick3/pick4 codes from slot.sourceCfg JSON (DB)
    String pick3Code = jsonText(slot.sourceCfg(), "pick3", "code");
    String pick4Code = jsonText(slot.sourceCfg(), "pick4", "code");

    boolean pick3Active = jsonBool(slot.sourceCfg(), true, "pick3", "active");
    boolean pick4Active = jsonBool(slot.sourceCfg(), true, "pick4", "active");

    Set<String> wanted = new LinkedHashSet<>();
    if (pick3Active && !pick3Code.isBlank()) wanted.add(norm(pick3Code));
    if (pick4Active && !pick4Code.isBlank()) wanted.add(norm(pick4Code));

    if (wanted.isEmpty()) {
      return new ExternalBundle(
          ExternalResultOutput.notFound(
              "SLOT_HAS_NO_GAMES",
              flags(
                  providerKey,
                  ExternalFetchStatus.NOT_FOUND,
                  ResultQuality.SUSPECT,
                  q.now(),
                  Map.of("slotKey", q.slotKey())),
              Map.of("slotKey", q.slotKey())),
          null,
          Map.of("slotKey", q.slotKey(), "provider", providerKey));
    }

    // Call provider once for both codes (for date)
    List<LatestDraw> draws;
    try {
      draws =
          client.fetchDraws(
              new ProviderDrawQuery(
                  q.date(),
                  wanted,
                  Math.min(Math.max(1, wanted.size()), 20),
                  q.force(),
                  q.dryRun()));
    } catch (Exception ex) {
      log.warn(
          "uslottery.fetchSlot provider_error provider={} slotKey={} date={} err={}",
          providerKey,
          q.slotKey(),
          q.date(),
          ex.toString());

      var errFlags =
          flags(
              providerKey,
              ExternalFetchStatus.ERROR,
              ResultQuality.SUSPECT,
              q.now(),
              Map.of("error", ex.getMessage()));
      ExternalResultOutput err =
          ExternalResultOutput.notFound(
              "PROVIDER_ERROR", errFlags, Map.of("error", ex.getMessage()));
      return new ExternalBundle(err, err, Map.of("provider", providerKey, "slotKey", q.slotKey()));
    }

    Map<String, LatestDraw> index = new HashMap<>();
    for (var d : draws) {
      if (d == null || d.channelCode() == null) continue;
      index.put(norm(d.channelCode()), d);
    }

    ExternalResultOutput pick3 = buildOutputForCode(providerKey, pick3Code, pick3Active, q, index);
    ExternalResultOutput pick4 = buildOutputForCode(providerKey, pick4Code, pick4Active, q, index);

    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("provider", providerKey);
    raw.put("slotKey", q.slotKey());
    raw.put("date", String.valueOf(q.date()));
    raw.put("wanted", List.copyOf(wanted));
    raw.put("count", draws == null ? 0 : draws.size());

    return new ExternalBundle(pick3, pick4, raw);
  }

  private ExternalResultOutput buildOutputForCode(
      String providerKey,
      String code,
      boolean active,
      ResultSlotFetchQuery q,
      Map<String, LatestDraw> index) {

    if (!active || safe(code).isBlank()) return null;

    String gameCode = norm(code);
    LatestDraw d = index.get(gameCode);

    if (d == null) {
      return ExternalResultOutput.notFound(
          "NOT_FOUND",
          flags(
              providerKey,
              ExternalFetchStatus.NOT_FOUND,
              ResultQuality.SUSPECT,
              q.now(),
              Map.of("code", gameCode)),
          Map.of("code", gameCode));
    }

    String sourceHash = "";
    if (d.meta() != null && d.meta().get("hash") != null)
      sourceHash = String.valueOf(d.meta().get("hash"));

    var f =
        new SourceFlags(
            IngestionMode.EXTERNAL,
            providerKey,
            ExternalFetchStatus.FOUND,
            d.quality(),
            d.origin(),
            sourceHash,
            "",
            q.now(),
            Map.of(
                "code",
                d.channelCode(),
                "gameKey",
                safe(d.externalGameKey()),
                "drawType",
                safe(d.externalDrawType())));

    return ExternalResultOutput.found(
        "FOUND",
        d.numbers() == null ? List.of() : d.numbers().ordered(),
        d.extras() == null
            ? List.of()
            : d.extras().extraNumbers().stream().map(String::valueOf).toList(),
        d.occurredAtUtc() == null ? null : d.occurredAtUtc().toInstant(),
        d.quality(),
        f,
        Map.of(
            "provider", providerKey,
            "channel_code", safe(d.channelCode()),
            "draw_date", String.valueOf(d.drawDate()),
            "origin", safe(d.origin())));
  }

  // ----------------------------
  // helpers
  // ----------------------------
  private UsLotteryProviderClient findProvider(String providerKey) {
    if (providerKey == null) return null;
    String pk = providerKey.trim().toUpperCase(Locale.ROOT);

    for (var p : providers) {
      if (p.provider().name().equalsIgnoreCase(pk)) return p;
    }
    for (var p : providers) {
      if (p.provider().name().replace("_", "").equalsIgnoreCase(pk.replace("_", ""))) return p;
    }
    return null;
  }

  private static SourceFlags flags(
      String providerKey,
      ExternalFetchStatus status,
      ResultQuality q,
      Instant now,
      Map<String, Object> meta) {
    return new SourceFlags(
        IngestionMode.EXTERNAL,
        safe(providerKey),
        status,
        q,
        safe(providerKey),
        "",
        "",
        now == null ? Instant.now() : now,
        meta == null ? Map.of() : meta);
  }

  private static String jsonText(JsonNode root, String obj, String field) {
    if (root == null) return "";
    JsonNode node = root.path(obj).path(field);
    return node.isMissingNode() || node.isNull() ? "" : node.asText("");
  }

  private static boolean jsonBool(JsonNode root, boolean def, String obj, String field) {
    if (root == null) return def;
    JsonNode node = root.path(obj).path(field);
    return node.isMissingNode() || node.isNull() ? def : node.asBoolean(def);
  }

  private static String norm(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }
}
