package com.tchalanet.server.core.uslottery.internal.infra.external.pa;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import com.tchalanet.server.core.uslottery.internal.infra.external.ProviderSlotCodeMatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Maps PA Lottery RSS feed entries to provider results.
 *
 * Expected RSS title format: "Pick 3 Midday - 06/14/2026" or "Pick 4 Evening - 06/14/2026"
 * Expected description format: winning numbers separated by spaces, dashes, or commas, e.g. "3 - 1 - 5"
 *
 * Adjust TITLE_DATE_FORMATTER and number parsing if the actual feed format differs.
 */
@Component
@Slf4j
public class PennsylvaniaDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.PA;
    private static final String ORIGIN = "PA_RSS";

    private static final DateTimeFormatter MDY = DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US);
    private static final Pattern NUMBER_SPLITTER = Pattern.compile("[\\s\\-,]+");

    public UsLotteryProviderResponse map(
        String xml,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query) {

        var feed = parseFeed(xml);
        if (feed.isEmpty()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var wantedCodes = normalizeSet(query.externalGameCodes());

        var results = mapEntriesSafely(feed.get().getEntries(), wantedCodes, sourceHash, url, query);

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            results,
            query.includeRaw() ? xml : null);
    }

    private List<UsLotteryProviderResult> mapEntriesSafely(
        List<SyndEntry> entries,
        Set<String> wantedCodes,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query) {

        var results = new ArrayList<UsLotteryProviderResult>();

        for (var entry : entries) {
            try {
                var raw = toRawDraw(entry);
                if (raw.isEmpty()) {
                    continue;
                }

                var draw = raw.get();

                if (!query.drawDate().equals(draw.drawDate())) {
                    continue;
                }

                if (!wantedCodes.isEmpty() && !wantedCodes.contains(draw.game().gameCode)) {
                    continue;
                }

                if (!ProviderSlotCodeMatcher.matches(draw.slot().name(), query.providerSlotCode())) {
                    continue;
                }

                toProviderResult(draw, sourceHash, url, query).ifPresent(results::add);

            } catch (Exception ex) {
                log.warn(
                    "pa-client skipped invalid entry provider={} drawDate={} providerSlotCode={} err={}",
                    PROVIDER,
                    query.drawDate(),
                    query.providerSlotCode(),
                    ex.getMessage(),
                    ex);
            }
        }

        return List.copyOf(results);
    }

    private Optional<PaRawDraw> toRawDraw(SyndEntry entry) {
        if (entry == null || StringUtils.isBlank(entry.getTitle())) {
            return Optional.empty();
        }

        var title = clean(entry.getTitle());
        var description = clean(entry.getDescription() == null ? "" : entry.getDescription().getValue());

        var game = resolveGame(title);
        if (game == null) {
            return Optional.empty();
        }

        var slot = resolveSlot(title);
        if (slot == null) {
            return Optional.empty();
        }

        var drawDate = resolveDate(title);
        if (drawDate.isEmpty()) {
            return Optional.empty();
        }

        var numbers = parseNumbers(description, game.expectedSize);
        if (numbers.isEmpty()) {
            return Optional.empty();
        }

        var publishedAt = entry.getPublishedDate() == null ? null : entry.getPublishedDate().toInstant();

        return Optional.of(new PaRawDraw(game, slot, drawDate.get(), numbers.get(), clean(entry.getLink()), publishedAt));
    }

    private Optional<UsLotteryProviderResult> toProviderResult(
        PaRawDraw raw,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query) {

        var quality = raw.main().size() == raw.game().expectedSize
            ? ResultQuality.COMPLETE
            : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", raw.game().gameCode);
        metadata.put("draw_date", String.valueOf(raw.drawDate()));
        metadata.put("provider_slot_code", raw.slot().name());
        metadata.put("expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));
        metadata.put("link", raw.link());

        if (raw.publishedAt() != null) {
            metadata.put("published_at", raw.publishedAt().toString());
        }

        var flags = new UsProviderSourceFlags(ORIGIN, sourceHash, clean(url), Map.copyOf(metadata));

        return Optional.of(new UsLotteryProviderResult(
            raw.game().gameCode,
            raw.main(),
            List.of(),
            quality,
            flags,
            raw.drawDate().atTime(raw.slot().drawTime).atZone(query.timezone()).toInstant(),
            query.includeRaw() ? raw : null));
    }

    private Optional<SyndFeed> parseFeed(String xml) {
        try (var input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
             var reader = new XmlReader(input)) {
            return Optional.of(new SyndFeedInput().build(reader));
        } catch (Exception e) {
            log.warn("pa-client parse rss failed err={}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static Game resolveGame(String title) {
        var normalized = title.toUpperCase(Locale.ROOT);
        if (normalized.contains("PICK 3") || normalized.contains("PICK3")) {
            return Game.PICK3;
        }
        if (normalized.contains("PICK 4") || normalized.contains("PICK4")) {
            return Game.PICK4;
        }
        return null;
    }

    private static Slot resolveSlot(String title) {
        var normalized = title.toUpperCase(Locale.ROOT);
        for (var slot : Slot.values()) {
            if (normalized.contains(slot.label)) {
                return slot;
            }
        }
        return null;
    }

    private static Optional<LocalDate> resolveDate(String title) {
        var marker = "- ";
        var idx = title.lastIndexOf(marker);
        if (idx < 0) {
            return Optional.empty();
        }

        var raw = title.substring(idx + marker.length()).trim();
        try {
            return Optional.of(LocalDate.parse(raw, MDY));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<List<String>> parseNumbers(String description, int expectedSize) {
        if (StringUtils.isBlank(description)) {
            return Optional.empty();
        }

        var parts = NUMBER_SPLITTER.split(description.trim());
        var numbers = Arrays.stream(parts)
            .map(String::trim)
            .filter(s -> !s.isBlank() && s.matches("\\d+"))
            .toList();

        if (numbers.size() < expectedSize) {
            return Optional.empty();
        }

        return Optional.of(numbers.subList(0, expectedSize));
    }

    private static Set<String> normalizeSet(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }
        return codes.stream()
            .filter(Objects::nonNull)
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .filter(s -> !s.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private enum Game {
        PICK3("PICK3", 3),
        PICK4("PICK4", 4);

        final String gameCode;
        final int expectedSize;

        Game(String gameCode, int expectedSize) {
            this.gameCode = gameCode;
            this.expectedSize = expectedSize;
        }
    }

    private enum Slot {
        MIDDAY("MIDDAY", LocalTime.of(13, 0)),
        EVENING("EVENING", LocalTime.of(18, 0));

        final String label;
        final LocalTime drawTime;

        Slot(String label, LocalTime drawTime) {
            this.label = label;
            this.drawTime = drawTime;
        }
    }

    private record PaRawDraw(
        Game game,
        Slot slot,
        LocalDate drawDate,
        List<String> main,
        String link,
        Instant publishedAt) {}
}
