package com.tchalanet.server.core.uslottery.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.contracts.results.ExternalFetchStatus;
import com.tchalanet.server.common.contracts.results.ExternalResultOutput;
import com.tchalanet.server.common.contracts.results.IngestionMode;
import com.tchalanet.server.common.contracts.results.SourceFlags;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.application.port.out.ExternalResultsFetchPort;
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
    private final ResultSlotCatalog resultSlotCatalog;

    @Override
    public ExternalBundle fetchSlot(ResultSlotFetchQuery q) {
        if (q == null) return new ExternalBundle(null, null, Map.of());

        var slotOpt = resultSlotCatalog.findByKey(norm(q.slotKey()));
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
        String providerKey = safe(slot.provider()); // "NY", "GA", ...
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
                Map.of("provider", providerKey, "slotKey", q.slotKey()));
        }

        // IMPORTANT:
        // Providers filter by LatestDraw.channelCode(), e.g. "US_NY_NUM3_MID" / "US_GA_CASH3_1229".
        // So we must query using source_cfg.*.external_game_code (seed contract).
        String pick3ChannelCode = jsonText(slot.sourceCfg(), "pick3", "external_game_code");
        String pick4ChannelCode = jsonText(slot.sourceCfg(), "pick4", "external_game_code");

        // Keep external_key only as metadata (NUMBERS/WIN4/PICK3/PICK4/CASH3/CASH4...)
        String pick3ExternalKey = jsonText(slot.sourceCfg(), "pick3", "external_key");
        String pick4ExternalKey = jsonText(slot.sourceCfg(), "pick4", "external_key");

        boolean pick3Active = jsonBool(slot.sourceCfg(), true, "pick3", "active");
        boolean pick4Active = jsonBool(slot.sourceCfg(), true, "pick4", "active");

        Set<String> wanted = new LinkedHashSet<>();
        if (pick3Active && !safe(pick3ChannelCode).isBlank()) wanted.add(norm(pick3ChannelCode));
        if (pick4Active && !safe(pick4ChannelCode).isBlank()) wanted.add(norm(pick4ChannelCode));

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
                Map.of(
                    "slotKey",
                    q.slotKey(),
                    "provider",
                    providerKey,
                    "pick3_channel",
                    pick3ChannelCode,
                    "pick4_channel",
                    pick4ChannelCode));
        }

        // Call provider once for both channels (for date)
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
                    Map.of("error", String.valueOf(ex.getMessage())));
            ExternalResultOutput err =
                ExternalResultOutput.notFound(
                    "PROVIDER_ERROR", errFlags, Map.of("error", String.valueOf(ex.getMessage())));
            return new ExternalBundle(err, err, Map.of("provider", providerKey, "slotKey", q.slotKey()));
        }

        Map<String, LatestDraw> index = new HashMap<>();
        for (var d : draws) {
            if (d == null || d.channelCode() == null) continue;
            index.put(norm(d.channelCode()), d);
        }

        ExternalResultOutput pick3 =
            buildOutputForChannel(providerKey, pick3ChannelCode, pick3ExternalKey, pick3Active, q, index);
        ExternalResultOutput pick4 =
            buildOutputForChannel(providerKey, pick4ChannelCode, pick4ExternalKey, pick4Active, q, index);

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", providerKey);
        raw.put("slotKey", q.slotKey());
        raw.put("date", String.valueOf(q.date()));
        raw.put("wanted", List.copyOf(wanted));
        raw.put("count", draws == null ? 0 : draws.size());
        raw.put(
            "pick3",
            Map.of(
                "channel_code",
                safe(pick3ChannelCode),
                "external_key",
                safe(pick3ExternalKey),
                "active",
                pick3Active));
        raw.put(
            "pick4",
            Map.of(
                "channel_code",
                safe(pick4ChannelCode),
                "external_key",
                safe(pick4ExternalKey),
                "active",
                pick4Active));

        return new ExternalBundle(pick3, pick4, raw);
    }

    private ExternalResultOutput buildOutputForChannel(
        String providerKey,
        String channelCode,
        String externalKey,
        boolean active,
        ResultSlotFetchQuery q,
        Map<String, LatestDraw> index) {

        if (!active || safe(channelCode).isBlank()) return null;

        String cc = norm(channelCode);
        LatestDraw d = index.get(cc);

        if (d == null) {
            return ExternalResultOutput.notFound(
                "NOT_FOUND",
                flags(
                    providerKey,
                    ExternalFetchStatus.NOT_FOUND,
                    ResultQuality.SUSPECT,
                    q.now(),
                    Map.of("channel_code", cc, "external_key", safe(externalKey))),
                Map.of("channel_code", cc, "external_key", safe(externalKey)));
        }

        String sourceHash = "";
        if (d.meta() != null && d.meta().get("hash") != null) sourceHash = String.valueOf(d.meta().get("hash"));

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
                    "channel_code",
                    safe(d.channelCode()),
                    "external_key",
                    safe(externalKey),
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
                "provider",
                providerKey,
                "channel_code",
                safe(d.channelCode()),
                "external_key",
                safe(externalKey),
                "draw_date",
                String.valueOf(d.drawDate()),
                "origin",
                safe(d.origin())));
    }

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
