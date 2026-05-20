package com.tchalanet.server.core.uslottery.internal.infra.external.ny;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import com.tchalanet.server.core.uslottery.internal.infra.external.ProviderSlotCodeMatcher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

@Component
@Slf4j
@RequiredArgsConstructor
public class NewYorkDrupalMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.NY;
    private static final String ORIGIN = "NY_DRUPAL_API";

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        Map<String, String> rawByGame,
        String url,
        UsLotteryProviderQuery query,
        String sourceStrategy
    ) {
        var results = new ArrayList<UsLotteryProviderResult>();
        var combinedRaw = query.includeRaw() ? new LinkedHashMap<String, Object>() : null;

        rawByGame.forEach((gameCode, body) -> {
            var payload = parsePayload(body);

            if (payload == null || payload.rows() == null || payload.rows().isEmpty()) {
                log.warn("ny-drupal no rows gameCode={} drawDate={}", gameCode, query.drawDate());
                return;
            }

            if (combinedRaw != null) {
                combinedRaw.put(gameCode, payload);
            }

            payload.rows().stream()
                .map(row -> toResult(gameCode, row, body, url, query, sourceStrategy))
                .filter(Objects::nonNull)
                .forEach(results::add);
        });

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.copyOf(results),
            combinedRaw);
    }

    private UsLotteryProviderResult toResult(
        String gameCode,
        NewYorkDrupalRow row,
        String body,
        String url,
        UsLotteryProviderQuery query,
        String sourceStrategy
    ) {
        if (row == null) {
            return null;
        }

        var rowDate = parseDate(row.date());
        if (!query.drawDate().equals(rowDate)) {
            return null;
        }

        var providerSlotCode = ProviderSlotCodeMatcher.normalize(row.drawTime());
        if (!ProviderSlotCodeMatcher.matches(providerSlotCode, query.providerSlotCode())) {
            return null;
        }

        var main = row.winningNumbers() == null
            ? List.<String>of()
            : row.winningNumbers().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (main.isEmpty()) {
            return null;
        }

        var expectedSize = expectedSize(gameCode);
        var quality = main.size() == expectedSize ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", gameCode);
        metadata.put("draw_date", String.valueOf(rowDate));
        metadata.put("provider_slot_code", providerSlotCode);
        metadata.put("expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));
        metadata.put("source_strategy", sourceStrategy);
        metadata.put("ny_row_id", String.valueOf(row.id()));
        metadata.put("ny_game_label", String.valueOf(row.game()));

        var flags =
            new UsProviderSourceFlags(
                ORIGIN,
                Hashing.sha256Hex(body),
                url,
                Map.copyOf(metadata));

        return new UsLotteryProviderResult(
            gameCode,
            main,
            List.of(),
            quality,
            flags,
            resolveOccurredAt(query),
            query.includeRaw() ? row : null);
    }

    private NewYorkDrupalPayload parsePayload(String body) {
        try {
            return json.fromJson(body, new TypeReference<NewYorkDrupalPayload>() {});
        } catch (Exception ex) {
            log.warn("ny-drupal parse failed: {}", ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static Instant resolveOccurredAt(UsLotteryProviderQuery query) {
        return query.drawDate()
            .atTime(query.drawTime())
            .atZone(query.timezone())
            .toInstant();
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(raw.trim().substring(0, 10));
        } catch (Exception ex) {
            log.warn("Failed to parse NY Drupal draw date '{}': {}", raw, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static int expectedSize(String gameCode) {
        return switch (gameCode) {
            case "NUMBERS" -> 3;
            case "WIN4" -> 4;
            default -> 0;
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewYorkDrupalPayload(
        @JsonProperty("rows") List<NewYorkDrupalRow> rows
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewYorkDrupalRow(
        @JsonProperty("id") String id,
        @JsonProperty("game") String game,
        @JsonProperty("date") String date,
        @JsonProperty("draw_time") String drawTime,
        @JsonProperty("winning_numbers") List<String> winningNumbers
    ) {}
}
