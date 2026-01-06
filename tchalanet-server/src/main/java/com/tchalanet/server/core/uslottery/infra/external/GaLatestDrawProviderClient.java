package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.uslottery.application.port.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.DrawExtras;
import com.tchalanet.server.core.uslottery.domain.model.DrawMain;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import java.time.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.ga",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class GaLatestDrawProviderClient implements LatestDrawProviderClient {

  private final org.springframework.web.client.RestClient gaRestClient;
  private final UsLotteryProperties props;
  private final JsonbUtils jsonbUtils;

  private final UsLotteryProviderRawCache cacheManager;

  public GaLatestDrawProviderClient(
      @Qualifier("gaLotteryRestClient") org.springframework.web.client.RestClient gaRestClient,
      UsLotteryProperties props,
      JsonbUtils jsonbUtils,
      UsLotteryProviderRawCache cacheManager) {
    this.gaRestClient = gaRestClient;
    this.props = props;
    this.jsonbUtils = jsonbUtils;
    this.cacheManager = cacheManager;
  }

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.GA;
  }

  @Override
  public List<LatestDraw> fetchLatestDraws() {
    var providerCfg = props.getProviders() != null ? props.getProviders().get("ga") : null;
    var tz = providerCfg != null ? providerCfg.getTimezone() : "America/New_York";
    var requestUrl =
        providerCfg != null ? providerCfg.getBaseUrl() + providerCfg.getLatestPath() : null;
    if (requestUrl == null || requestUrl.isBlank()) return List.of();
    var zone = ZoneId.of(tz);

    var drawDate = ZonedDateTime.now(zone).toLocalDate();

    var queryHash = sha256(requestUrl);

    var body =
        cacheManager.getOrFetch(
            provider().name(),
            drawDate,
            queryHash,
            () -> {
              try {
                return gaRestClient.get().uri(requestUrl).retrieve().body(String.class);
              } catch (Exception ex) {
                log.warn("ga-latest-client: fetch failed: {}", ex.getMessage(), ex);
                return null;
              }
            });

    if (body == null || body.isBlank()) return List.of();

    return parseGaBody(body, requestUrl, zone);
  }

  private List<LatestDraw> parseGaBody(String body, String latestPath, ZoneId zone) {
    List<LatestDraw> results = new ArrayList<>();
    try {
      JsonNode root = jsonbUtils.readTree(body);
      // Georgia API returns an array of draws or an object with a 'results' array.
      List<GaEntryDto> entries;
      if (root != null && root.isArray()) {
        entries =
            jsonbUtils.fromJson(body, new com.fasterxml.jackson.core.type.TypeReference<>() {});
      } else {
        JsonNode resultsNode = root == null ? null : root.get("results");
        if (resultsNode != null && resultsNode.isArray()) {
          entries =
              jsonbUtils.convertValue(
                  resultsNode, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } else {
          entries = List.of();
        }
      }

      Instant fetchedAt = Instant.now();
      for (GaEntryDto e : entries) {
        processEntry(results, e, latestPath, zone, fetchedAt, body);
      }
    } catch (Exception ex) {
      log.warn("ga-latest-client: parse failed: {}", ex.toString());
    }
    return results;
  }

  private void processEntry(
      List<LatestDraw> results,
      GaEntryDto e,
      String latestPath,
      ZoneId zone,
      Instant fetchedAt,
      String rawBody) {
    String gameKey = normalizeGameKey(e.gameName());
    if (gameKey == null) return;

    // map some common game keys
    // CASH 3 -> CASH3 -> PICK3 semantics, CASH 4 -> PICK4, etc.
    String mappedKey =
        switch (gameKey) {
          case "CASH3", "CASH 3" -> "PICK3";
          case "CASH4", "CASH 4" -> "PICK4";
          case "CASHPOP", "CASH POP" -> "POP";
          default -> gameKey;
        };

    // prefer top-level name (ex: "NIGHT", "MIDDAY", "EVENING")
    String drawType = normalize(e.name());
    var firstResult = e.results() == null ? null : e.results().stream().findFirst().orElse(null);
    if (drawType == null && firstResult != null) {
      drawType = normalize(firstResult.drawType());
    }

    // prefer drawTime millis if provided, otherwise try drawDate string
    LocalDate date;
    if (e.drawTime() != null) {
      try {
        date = Instant.ofEpochMilli(e.drawTime()).atZone(zone).toLocalDate();
      } catch (Exception ex) {
        // fallback
        date = parseGaDate(e.drawDate());
      }
    } else {
      date = parseGaDate(e.drawDate());
    }

    if (date == null) return;

    String channelCode = mapChannelCode(mappedKey, drawType);
    if (channelCode == null) return;

    ParseResult pr = parseNumbersFromResult(e);
    if (pr.main().isEmpty()) return;

    OffsetDateTime occurredAt;
    if (e.drawTime() != null) {
      try {
        occurredAt = Instant.ofEpochMilli(e.drawTime()).atZone(zone).toOffsetDateTime();
      } catch (Exception ex) {
        occurredAt = date.atStartOfDay(zone).toOffsetDateTime();
      }
    } else {
      occurredAt = date.atStartOfDay(zone).toOffsetDateTime();
    }
    DrawExtras extras = new DrawExtras(pr.extraNumbers(), pr.attributes());
    String sourceHash = sha256(rawBody);

    buildAndAddLatestDraw(
        results,
        mappedKey,
        drawType,
        channelCode,
        date,
        occurredAt,
        fetchedAt,
        pr.main(),
        extras,
        latestPath,
        sourceHash);
  }

  private String mapChannelCode(String gameKey, String drawType) {
    return switch (gameKey) {
      case "PICK3" ->
          switch (drawType) {
            case "MIDDAY" -> "US_GA_CASH3_MID";
            case "EVENING", "NIGHT" -> "US_GA_CASH3_EVE";
            default -> null;
          };
      case "PICK4" ->
          switch (drawType) {
            case "MIDDAY" -> "US_GA_CASH4_MID";
            case "EVENING", "NIGHT" -> "US_GA_CASH4_EVE";
            default -> null;
          };
      default -> null;
    };
  }

  private ParseResult parseNumbersFromResult(GaEntryDto e) {
    if (e == null || e.results() == null || e.results().isEmpty())
      return new ParseResult(List.of(), List.of(), Map.of());
    var r = e.results().stream().findFirst().orElse(null);
    List<String> main = r == null || r.primary() == null ? List.of() : new ArrayList<>(r.primary());

    // validate numeric content and trim
    List<String> validatedMain = new ArrayList<>();
    for (String s : main) {
      if (s == null) continue;
      String t = s.trim();
      if (t.isEmpty()) continue;
      validatedMain.add(t);
    }

    return new ParseResult(List.copyOf(validatedMain), List.of(), Map.of());
  }

  private void buildAndAddLatestDraw(
      List<LatestDraw> results,
      String gameKey,
      String drawType,
      String channelCode,
      LocalDate date,
      OffsetDateTime occurredAt,
      Instant fetchedAt,
      List<String> digits,
      DrawExtras extras,
      String latestPath,
      String sourceHash) {
    try {
      DrawMain main = new DrawMain(digits);

      int expected =
          switch (gameKey) {
            case "PICK3" -> 3;
            case "PICK4" -> 4;
            default -> digits.size();
          };
      main.requireSize(expected, gameKey + "_" + drawType);

      results.add(
          new LatestDraw(
              provider(),
              gameKey,
              drawType,
              channelCode,
              date,
              occurredAt,
              fetchedAt,
              main,
              extras,
              ResultQuality.COMPLETE,
              "GA_API",
              Map.of("path", latestPath, "hash", sourceHash)));
    } catch (Exception ex) {
      results.add(
          new LatestDraw(
              provider(),
              gameKey,
              drawType,
              channelCode,
              date,
              occurredAt,
              fetchedAt,
              new DrawMain(digits),
              extras,
              ResultQuality.SUSPECT,
              "GA_API",
              Map.of("path", latestPath, "hash", sourceHash, "error", ex.getMessage())));
    }
  }

  private static String sha256(String s) {
    try {
      var md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] dig =
          md.digest((s == null ? "" : s).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      var sb = new StringBuilder(dig.length * 2);
      for (byte b : dig) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      return "NO_HASH";
    }
  }

  // ---- helpers + DTOs ----
  private record ParseResult(
      List<String> main, List<Integer> extraNumbers, Map<String, String> attributes) {}

  private static String normalizeGameKey(String gameName) {
    if (gameName == null) return null;
    return gameName.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  private static String normalize(String s) {
    if (s == null) return null;
    String t = s.trim().toUpperCase(Locale.ROOT);
    return t.isBlank() ? null : t;
  }

  private static LocalDate parseGaDate(String raw) {
    if (raw == null || raw.isBlank()) return null;
    // GA API may provide date-only or ISO date/time; try parse the date part
    try {
      return LocalDate.parse(raw.substring(0, 10));
    } catch (Exception ex) {
      try {
        return LocalDate.parse(raw);
      } catch (Exception e) {
        return null;
      }
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record GaEntryDto(
      @JsonProperty("drawDate") String drawDate,
      @JsonProperty("gameName") String gameName,
      @JsonProperty("name") String name,
      @JsonProperty("drawTime") Long drawTime,
      @JsonProperty("results") List<GaResultDto> results) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record GaResultDto(
      @JsonProperty("primary") List<String> primary,
      @JsonProperty("primaryRevealOrder") List<String> primaryRevealOrder,
      @JsonProperty("drawType") String drawType) {}
}
