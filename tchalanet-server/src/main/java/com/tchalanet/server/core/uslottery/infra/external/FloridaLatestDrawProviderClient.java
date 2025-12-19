package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.core.uslottery.application.port.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.*;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tch.us-lottery.providers.florida", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class FloridaLatestDrawProviderClient implements LatestDrawProviderClient {

    private final WebClient floridaLotteryWebClient;
    private final UsLotteryProperties props;

    @Override
    public UsLotteryProvider provider() {
        return UsLotteryProvider.FLORIDA;
    }

    @Override
    public List<LatestDraw> fetchLatestDraws() {
        List<LatestDraw> results = new ArrayList<>();

        var providerCfg = props.getProviders() != null ? props.getProviders().get("florida") : null;
        String tz = providerCfg != null ? providerCfg.getTimezone() : "America/New_York";
        String latestPath = providerCfg != null ? providerCfg.getLatestPath() : "/drawgamesapp/getLatestDrawGames";
        ZoneId zone = ZoneId.of(tz);

        try {
            FloridaResultDto response = floridaLotteryWebClient
                .get()
                .uri(latestPath)
                .retrieve()
                .bodyToMono(FloridaResultDto.class)
                .block();

            if (response == null || response.drawResults == null) {
                return results;
            }

            Instant fetchedAt = Instant.now();

            for (FloridaDrawGameDto game : response.drawResults) {
                processGame(results, game, latestPath, zone, fetchedAt);
            }
        } catch (Exception e) {
            log.warn("fl-latest-client: failed: {}", e.toString());
        }

        return results;
    }

    private void processGame(
        List<LatestDraw> results,
        FloridaDrawGameDto game,
        String latestPath,
        ZoneId zone,
        Instant fetchedAt
    ) {
        String gameName = game.gameName();
        if (gameName == null) {
            return;
        }

        String externalGameKey = gameName.trim().toUpperCase().replace(" ", ""); // PICK3/PICK4/...
        if (!isSupportedGame(externalGameKey)) {
            return;
        }

        if (game.draws() == null || game.draws().isEmpty()) {
            return;
        }

        for (FloridaDrawDto d : game.draws()) {
            processDraw(results, externalGameKey, d, latestPath, zone, fetchedAt);
        }
    }

    private boolean isSupportedGame(String externalGameKey) {
        return "PICK3".equals(externalGameKey) || "PICK4".equals(externalGameKey);
    }

    private void processDraw(
        List<LatestDraw> results,
        String externalGameKey,
        FloridaDrawDto d,
        String latestPath,
        ZoneId zone,
        Instant fetchedAt
    ) {
        LocalDate date = d.getDrawDate();
        String externalDrawType = normalize(d.getDrawType()); // MIDDAY/EVENING
        if (externalDrawType == null) {
            return;
        }

        String channelCode = mapChannelCode(externalGameKey, externalDrawType);
        if (channelCode == null) {
            return;
        }

        List<String> digits = parseDigits(d.drawNumbers());
        if (digits.isEmpty()) {
            return;
        }

        // occurredAtUtc: FL endpoint ne donne pas l'heure exacte -> on met minuit du provider
        OffsetDateTime occurredAtUtc = date.atStartOfDay(zone).toOffsetDateTime();

        buildAndAddLatestDraw(
            results,
            externalGameKey,
            externalDrawType,
            channelCode,
            date,
            occurredAtUtc,
            fetchedAt,
            digits,
            latestPath
        );
    }

    private void buildAndAddLatestDraw(
        List<LatestDraw> results,
        String externalGameKey,
        String externalDrawType,
        String channelCode,
        LocalDate date,
        OffsetDateTime occurredAtUtc,
        Instant fetchedAt,
        List<String> digits,
        String latestPath
    ) {
        try {
            DrawMain main = new DrawMain(digits);
            int expected = externalGameKey.equals("PICK3") ? 3 : 4;
            main.requireSize(expected, externalGameKey + "_" + externalDrawType);

            results.add(
                new LatestDraw(
                    UsLotteryProvider.FLORIDA,
                    externalGameKey,
                    externalDrawType,
                    channelCode,
                    date,
                    occurredAtUtc,
                    fetchedAt,
                    main,
                    DrawExtras.empty(),
                    ResultQuality.COMPLETE,
                    "FL_APIM",
                    Map.of("path", latestPath)
                )
            );
        } catch (Exception ex) {
            // si tu préfères ignorer plutôt que marquer SUSPECT, tu peux juste return;
            log.debug(
                "florida: suspect row ignored: game={} type={} date={} err={}",
                externalGameKey,
                externalDrawType,
                date,
                ex.toString()
            );
            results.add(
                new LatestDraw(
                    UsLotteryProvider.FLORIDA,
                    externalGameKey,
                    externalDrawType,
                    channelCode,
                    date,
                    occurredAtUtc,
                    fetchedAt,
                    new DrawMain(digits), // peut throw si digits invalides; sinon ok
                    DrawExtras.empty(),
                    ResultQuality.SUSPECT,
                    "FL_APIM",
                    Map.of("path", latestPath, "error", ex.getMessage())
                )
            );
        }
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase();
        if (t.isBlank()) return null;
        return t;
    }

    private String mapChannelCode(String externalGameKey, String externalDrawType) {
        return switch (externalGameKey + "_" + externalDrawType) {
            case "PICK3_MIDDAY" -> "US_FL_NUM3_MID";
            case "PICK3_EVENING" -> "US_FL_NUM3_EVE";
            case "PICK4_MIDDAY" -> "US_FL_NUM4_MID";
            case "PICK4_EVENING" -> "US_FL_NUM4_EVE";
            default -> null;
        };
    }

    private List<String> parseDigits(List<FloridaNumberDto> drawNumbers) {
        if (drawNumbers == null || drawNumbers.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (FloridaNumberDto n : drawNumbers) {
            String type = n.numberType();
            if (type != null && type.startsWith("wn")) {
                out.add(String.valueOf(n.numberPick()));
            }
        }
        return out;
    }

    // ---- DTOs ----
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FloridaResultDto(@JsonProperty("DrawResults") List<FloridaDrawGameDto> drawResults) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FloridaDrawGameDto(@JsonProperty("GameName") String gameName,
                                      @JsonProperty("Draws") List<FloridaDrawDto> draws) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FloridaDrawDto {
        @JsonProperty("DrawDate") private String drawDate;
        @JsonProperty("DrawType") private String drawType;
        @JsonProperty("DrawNumbers") private List<FloridaNumberDto> drawNumbers;

        LocalDate getDrawDate() { return LocalDate.parse(drawDate.substring(0, 10)); }
        String getDrawType() { return drawType; }
        List<FloridaNumberDto> drawNumbers() { return drawNumbers; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FloridaNumberDto(@JsonProperty("NumberPick") int numberPick,
                                    @JsonProperty("NumberType") String numberType) {}
}
