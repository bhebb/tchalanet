package com.tchalanet.server.core.uslottery.internal.infra.external.tx;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TexasDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.TX;
    private static final String ORIGIN = "TX_RSS";
    private static final DateTimeFormatter MDY = DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US);

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

        var results =
            feed.get().getEntries().stream()
                .map(this::toRawDraw)
                .flatMap(Optional::stream)
                .filter(raw -> query.drawDate().equals(raw.drawDate()))
                .filter(raw -> wantedCodes.isEmpty() || wantedCodes.contains(raw.game().gameCode))
                .map(raw -> toProviderResult(raw, sourceHash, url, query))
                .flatMap(Optional::stream)
                .toList();

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            results,
            query.includeRaw() ? xml : null);
    }

    private Optional<SyndFeed> parseFeed(String xml) {
        try (var input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
             var reader = new XmlReader(input)) {
            return Optional.of(new SyndFeedInput().build(reader));
        } catch (Exception e) {
            log.warn("tx-client parse rss failed err={}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Optional<TxRawDraw> toRawDraw(SyndEntry entry) {
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

        var result = parseDescription(description, game);
        if (result.isEmpty()) {
            return Optional.empty();
        }

        var publishedAt =
            entry.getPublishedDate() == null ? null : entry.getPublishedDate().toInstant();

        return Optional.of(
            new TxRawDraw(
                game,
                slot,
                drawDate.get(),
                result.get().main(),
                result.get().fireball(),
                clean(entry.getLink()),
                publishedAt));
    }

    private Optional<UsLotteryProviderResult> toProviderResult(
        TxRawDraw raw,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query) {

        var expectedSize = raw.game().expectedSize;
        var quality = raw.main().size() == expectedSize ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

        var metadata =
            new java.util.LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", raw.game().gameCode);
        metadata.put("draw_date", String.valueOf(raw.drawDate()));
        metadata.put("provider_slot", raw.slot().name());
        metadata.put("link", clean(raw.link()));

        if (raw.publishedAt() != null) {
            metadata.put("published_at", raw.publishedAt().toString());
        }

        var flags =
            new UsProviderSourceFlags(
                ORIGIN,
                sourceHash,
                clean(url),
                Map.copyOf(metadata));

        var extras = StringUtils.isBlank(raw.fireball()) ? List.<String>of() : List.of(raw.fireball().trim());

        var occurredAt = resolveOccurredAt(raw, query);

        return Optional.of(
            new UsLotteryProviderResult(
                raw.game().gameCode,
                raw.main(),
                extras,
                quality,
                flags,
                occurredAt,
                query.includeRaw() ? raw : null));
    }

    private static Instant resolveOccurredAt(TxRawDraw raw, UsLotteryProviderQuery query) {
        // Le slot métier reste source de vérité. Ici on retourne l'instant TX provider si utile.
        // Si drawresult préfère OccurredAtResolver(date + slot.drawTime + slot.timezone), il peut ignorer ce champ.
        return raw.drawDate().atTime(raw.slot().drawTime).atZone(query.timezone()).toInstant();
    }

    private static Game resolveGame(String title) {
        var normalized = title.toUpperCase(Locale.ROOT);

        if (normalized.startsWith("PICK 3 ")) {
            return Game.PICK3;
        }

        if (normalized.startsWith("DAILY 4 ")) {
            return Game.DAILY4;
        }

        return null;
    }

    private static Slot resolveSlot(String title) {
        var normalized = title.toUpperCase(Locale.ROOT);

        for (var slot : Slot.values()) {
            if (normalized.contains(" " + slot.label + " WINNING NUMBERS ")) {
                return slot;
            }
        }

        return null;
    }

    private static Optional<LocalDate> resolveDate(String title) {
        var marker = " for ";
        var idx = title.toLowerCase(Locale.ROOT).lastIndexOf(marker);

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

    private static Optional<ParsedResult> parseDescription(String description, Game game) {
        if (StringUtils.isBlank(description)) {
            return Optional.empty();
        }

        var parts = description.toUpperCase(Locale.ROOT).split("FIREBALL", 2);
        var numbersPart = clean(parts[0]);
        var fireball = parts.length > 1 ? clean(parts[1]) : "";

        var main =
            Arrays.stream(numbersPart.split("-"))
                .map(TexasDrawResultsMapper::clean)
                .filter(s -> !s.isBlank())
                .toList();

        if (main.size() != game.expectedSize) {
            return Optional.empty();
        }

        return Optional.of(new ParsedResult(main, fireball));
    }

    private static Set<String> normalizeSet(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }

        return codes.stream()
            .filter(Objects::nonNull)
            .map(TexasDrawResultsMapper::normalize)
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private enum Game {
        PICK3("PICK3", 3),
        DAILY4("DAILY4", 4);

        private final String gameCode;
        private final int expectedSize;

        Game(String gameCode, int expectedSize) {
            this.gameCode = gameCode;
            this.expectedSize = expectedSize;
        }
    }

    private enum Slot {
        MORNING("MORNING", LocalTime.of(10, 0)),
        DAY("DAY", LocalTime.of(12, 27)),
        EVENING("EVENING", LocalTime.of(18, 0)),
        NIGHT("NIGHT", LocalTime.of(22, 12));

        private final String label;
        private final LocalTime drawTime;

        Slot(String label, LocalTime drawTime) {
            this.label = label;
            this.drawTime = drawTime;
        }
    }

    private record TxRawDraw(
        Game game,
        Slot slot,
        LocalDate drawDate,
        List<String> main,
        String fireball,
        String link,
        Instant publishedAt) {}

    private record ParsedResult(List<String> main, String fireball) {}
}
