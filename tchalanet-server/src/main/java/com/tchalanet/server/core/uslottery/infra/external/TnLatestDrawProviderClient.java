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
    prefix = "tch.us-lottery.providers.tn",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class TnLatestDrawProviderClient implements LatestDrawProviderClient {

  private final org.springframework.web.client.RestClient tnRestClient;
  private final UsLotteryProperties props;
  private final JsonbUtils jsonbUtils;

  private final UsLotteryProviderRawCache cacheManager;

  public TnLatestDrawProviderClient(
      @Qualifier("tnLotteryRestClient") org.springframework.web.client.RestClient tnRestClient,
      UsLotteryProperties props,
      JsonbUtils jsonbUtils,
      UsLotteryProviderRawCache cacheManager) {
    this.tnRestClient = tnRestClient;
    this.props = props;
    this.jsonbUtils = jsonbUtils;
    this.cacheManager = cacheManager;
  }

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.TN;
  }

  @Override
  public List<LatestDraw> fetchLatestDraws() {
    var providerCfg = props.getProviders() != null ? props.getProviders().get("tn") : null;
    var tz = providerCfg != null ? providerCfg.getTimezone() : "America/Chicago";
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
                return tnRestClient.get().uri(requestUrl).retrieve().body(String.class);
              } catch (Exception ex) {
                log.warn("tn-latest-client: fetch failed: {}", ex.getMessage(), ex);
                return null;
              }
            });

    if (body == null || body.isBlank()) return List.of();

    return parseTnBody(body, requestUrl, zone);
  }

  private List<LatestDraw> parseTnBody(String body, String latestPath, ZoneId zone) {
    // Jackpocket returns an object with 'lottery_results' array; fallback to results/data
    List<LatestDraw> results = new ArrayList<>();
    try {
      JsonNode root = jsonbUtils.readTree(body);
      List<TnEntryDto> entries;

      if (root != null && root.has("lottery_results") && root.get("lottery_results").isArray()) {
        entries =
            jsonbUtils.convertValue(
                root.get("lottery_results"),
                new com.fasterxml.jackson.core.type.TypeReference<>() {});
      } else if (root != null && root.isArray()) {
        entries =
            jsonbUtils.fromJson(body, new com.fasterxml.jackson.core.type.TypeReference<>() {});
      } else {
        JsonNode resultsNode = root == null ? null : root.get("results");
        if (resultsNode != null && resultsNode.isArray()) {
          entries =
              jsonbUtils.convertValue(
                  resultsNode, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } else {
          JsonNode dataNode = root == null ? null : root.get("data");
          if (dataNode != null && dataNode.isArray()) {
            entries =
                jsonbUtils.convertValue(
                    dataNode, new com.fasterxml.jackson.core.type.TypeReference<>() {});
          } else {
            entries = List.of();
          }
        }
      }

      Instant fetchedAt = Instant.now();
      for (TnEntryDto e : entries) {
        processEntry(results, e, latestPath, zone, fetchedAt, body);
      }
    } catch (Exception ex) {
      log.warn("tn-latest-client: parse failed: {}", ex.toString());
    }
    return results;
  }

  private void processEntry(
      List<LatestDraw> results,
      TnEntryDto e,
      String latestPath,
      ZoneId zone,
      Instant fetchedAt,
      String rawBody) {
    if (e == null) return;

    // lottery_name is the human name (e.g. "Cash 4 Life", "Lotto America")
    String lotteryName = e.lotteryName();
    if (lotteryName == null) return;

    String gameKey = normalizeGameKey(lotteryName);
    if (gameKey == null || gameKey.isBlank()) return;

    // map to internal keys if necessary; default to gameKey
    String mappedKey =
        switch (gameKey) {
          case "CASH4LIFE", "CASH4_LIFE" -> "CASH4LIFE";
          default -> gameKey;
        };

    // draw type: try name or next/first result drawType
    String drawType = normalize(e.name());
    var firstResult = e.results() == null ? null : e.results().stream().findFirst().orElse(null);
    if (drawType == null && firstResult != null) drawType = normalize(firstResult.drawType());
    if (drawType == null) drawType = "DEFAULT";

    // drawing_date is epoch seconds
    if (e.drawingDate() == null) return;
    LocalDate date;
    try {
      date = Instant.ofEpochSecond(e.drawingDate()).atZone(zone).toLocalDate();
    } catch (Exception ex) {
      return;
    }

    // channel code: build a sane code from lottery name
    String channelCode = "US_TN_" + gameKey.replaceAll("[^A-Z0-9]", "_");

    ParseResult pr = parseNumbersFromResult(e);
    if (pr.main().isEmpty()) return;

    OffsetDateTime occurredAt;
    try {
      occurredAt = Instant.ofEpochSecond(e.drawingDate()).atZone(zone).toOffsetDateTime();
    } catch (Exception ex) {
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

  private ParseResult parseNumbersFromResult(TnEntryDto e) {
    if (e == null) return new ParseResult(List.of(), List.of(), Map.of());

    List<String> main = new ArrayList<>();
    List<Integer> extraNums = new ArrayList<>();

    // Prefer structured numbers object if present
    if (e.numbers() != null) {
      var n = e.numbers();
      if (n.standard() != null && !n.standard().isEmpty()) {
        main.addAll(n.standard());
      }
      if (n.cashball() != null) {
        for (String s : n.cashball()) {
          try {
            extraNums.add(Integer.parseInt(s));
          } catch (Exception ignored) {
          }
        }
      }
      if (n.starBall() != null) {
        for (String s : n.starBall()) {
          try {
            extraNums.add(Integer.parseInt(s));
          } catch (Exception ignored) {
          }
        }
      }
      if (n.allStarBonus() != null) {
        for (String s : n.allStarBonus()) {
          try {
            extraNums.add(Integer.parseInt(s));
          } catch (Exception ignored) {
          }
        }
      }
    } else {
      // fallback to legacy results.primary
      var r = e.results() == null ? null : e.results().stream().findFirst().orElse(null);
      if (r != null && r.primary() != null) {
        main.addAll(r.primary());
      }
    }

    // trim main entries
    List<String> validatedMain = new ArrayList<>();
    for (String s : main) {
      if (s == null) continue;
      String t = s.trim();
      if (t.isEmpty()) continue;
      validatedMain.add(t);
    }

    return new ParseResult(List.copyOf(validatedMain), List.copyOf(extraNums), Map.of());
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
              "TN_API",
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
              "TN_API",
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record TnEntryDto(
      @JsonProperty("drawing_date") Long drawingDate,
      @JsonProperty("numbers") TnNumbersDto numbers,
      @JsonProperty("lottery_name") String lotteryName,
      @JsonProperty("lottery_id") Integer lotteryId,
      @JsonProperty("next_drawing_date") Long nextDrawingDate,
      @JsonProperty("id") Long id,
      @JsonProperty("name") String name,
      @JsonProperty("results") List<TnResultDto> results) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record TnNumbersDto(
      @JsonProperty("standard") List<String> standard,
      @JsonProperty("cashball") List<String> cashball,
      @JsonProperty("star_ball") List<String> starBall,
      @JsonProperty("all_star_bonus") List<String> allStarBonus) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record TnResultDto(
      @JsonProperty("primary") List<String> primary, @JsonProperty("drawType") String drawType) {}
}
