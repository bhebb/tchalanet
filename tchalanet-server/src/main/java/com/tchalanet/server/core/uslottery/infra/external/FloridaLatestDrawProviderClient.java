package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.JsonbUtils;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
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
    prefix = "tch.us-lottery.providers.florida",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class FloridaLatestDrawProviderClient implements LatestDrawProviderClient {

  private final org.springframework.web.client.RestClient floridaRestClient;
  private final UsLotteryProperties props;
  private final JsonbUtils jsonbUtils;

  private final UsLotteryProviderRawCache cacheManager;

  public FloridaLatestDrawProviderClient(
      @Qualifier("floridaLotteryRestClient")
          org.springframework.web.client.RestClient floridaRestClient,
      UsLotteryProperties props,
      JsonbUtils jsonbUtils,
      UsLotteryProviderRawCache cacheManager) {
    this.floridaRestClient = floridaRestClient;
    this.props = props;
    this.jsonbUtils = jsonbUtils;
    this.cacheManager = cacheManager;
  }

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.FL; // assure-toi que l'enum s'appelle vraiment FL
  }

  @Override
  public List<LatestDraw> fetchLatestDraws() {
    var providerCfg = props.getProviders() != null ? props.getProviders().get("fl") : null;
    var tz = providerCfg != null ? providerCfg.getTimezone() : "America/New_York";
    var requestFlDrawResults =
        providerCfg != null ? providerCfg.getBaseUrl() + providerCfg.getLatestPath() : null;
    var zone = ZoneId.of(tz);

    // Florida renvoie "latest" (souvent aujourd'hui). On cache par "drawDateLocal = today(zone)".
    var drawDate = ZonedDateTime.now(zone).toLocalDate();

    // use requestFlDrawResults as query identity (could be improved)
    var queryHash = sha256(requestFlDrawResults == null ? "latest" : requestFlDrawResults);

    var body =
        cacheManager.getOrFetch(
            provider().name(),
            drawDate,
            queryHash,
            () -> {
              try {
                return floridaRestClient
                    .get()
                    .uri(requestFlDrawResults)
                    .retrieve()
                    .body(String.class);
              } catch (Exception ex) {
                log.warn("fl-latest-client: fetch failed: {}", ex.getMessage(), ex);
                return null;
              }
            });

    if (body == null || body.isBlank()) return List.of();

    return parseFloridaBody(body, requestFlDrawResults, zone);
  }

  private List<LatestDraw> parseFloridaBody(String body, String latestPath, ZoneId zone) {
    List<LatestDraw> results = new ArrayList<>();
    try {
      JsonNode root = jsonbUtils.readTree(body);

      List<FloridaEntryDto> entries;
      if (root != null && root.isArray()) {
        entries =
            jsonbUtils.fromJson(
                body,
                new com.fasterxml.jackson.core.type.TypeReference<List<FloridaEntryDto>>() {});
      } else {
        JsonNode dr = root == null ? null : root.get("DrawResults");
        if (dr != null && dr.isArray()) {
          entries =
              jsonbUtils.convertValue(
                  dr,
                  new com.fasterxml.jackson.core.type.TypeReference<List<FloridaEntryDto>>() {});
        } else {
          entries = List.of();
        }
      }

      Instant fetchedAt = Instant.now();
      for (FloridaEntryDto e : entries) {
        processEntry(results, e, latestPath, zone, fetchedAt, body);
      }
    } catch (Exception ex) {
      log.warn("fl-latest-client: parse failed: {}", ex.toString());
    }
    return results;
  }

  private void processEntry(
      List<LatestDraw> results,
      FloridaEntryDto e,
      String latestPath,
      ZoneId zone,
      Instant fetchedAt,
      String rawBody // pour hash
      ) {
    String gameKey = normalizeGameKey(e.gameName());
    if (!Set.of("PICK3", "PICK4", "LOTTO").contains(gameKey)) return;

    LocalDate date = parseFloridaDate(e.drawDate());
    if (date == null) return;

    // ✅ LOTTO: drawType souvent pas utile -> on accepte null et on force DEFAULT
    String drawType = normalize(e.drawType());
    if ("LOTTO".equals(gameKey) && (drawType == null || drawType.isBlank())) {
      drawType = "DEFAULT";
    }
    if (drawType == null) return; // pour PICK3/4 on garde strict

    String channelCode = mapChannelCode(gameKey, drawType);
    if (channelCode == null) return;

    ParseResult pr = parseNumbers(e.drawNumbers(), gameKey);
    if (pr.main().isEmpty()) return;

    OffsetDateTime occurredAt = date.atStartOfDay(zone).toOffsetDateTime();

    DrawExtras extras = new DrawExtras(pr.extraNumbers(), pr.attributes());

    // ✅ Hash stable (payload utile). Ici: body complet (MVP) – tu peux améliorer plus tard.
    String sourceHash = sha256(rawBody);

    buildAndAddLatestDraw(
        results,
        gameKey,
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

  // ---- LOTTO FIXES ----

  private String mapChannelCode(String gameKey, String drawType) {
    return switch (gameKey) {
      case "PICK3" ->
          switch (drawType) {
            case "MIDDAY" -> "US_FL_PICK3_MID";
            case "EVENING" -> "US_FL_PICK3_EVE";
            default -> null;
          };
      case "PICK4" ->
          switch (drawType) {
            case "MIDDAY" -> "US_FL_PICK4_MID";
            case "EVENING" -> "US_FL_PICK4_EVE";
            default -> null;
          };
      case "LOTTO" -> "US_FL_LOTTO"; // ✅ un seul channel
      default -> null;
    };
  }

  private ParseResult parseNumbers(List<FloridaNumberDto> drawNumbers, String gameKey) {
    if (drawNumbers == null || drawNumbers.isEmpty())
      return new ParseResult(List.of(), List.of(), Map.of());

    int expected =
        switch (gameKey) {
          case "PICK3" -> 3;
          case "PICK4" -> 4;
          case "LOTTO" -> 6; // ✅ LOTTO typique
          default -> 0;
        };

    record Wn(int idx, String val) {}
    List<Wn> wn = new ArrayList<>();
    List<Integer> extraNums = new ArrayList<>();
    Map<String, String> attrs = new HashMap<>();

    for (FloridaNumberDto n : drawNumbers) {
      if (n == null) continue;
      String type = n.numberType();
      String pick = n.numberPick() == null ? "" : n.numberPick().trim();
      if (pick.isBlank()) continue;

      String t = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);

      // main numbers
      if (t.startsWith("wn")) {
        int idx = 99;
        try {
          idx = Integer.parseInt(t.substring(2));
        } catch (Exception ignored) {
        }
        wn.add(new Wn(idx, pick));
        continue;
      }

      // extras
      try {
        extraNums.add(Integer.parseInt(pick));
      } catch (NumberFormatException ignored) {
      }
      attrs.merge(t.isBlank() ? "extra" : t, pick, (a, b) -> a + "," + b);
    }

    wn.sort(Comparator.comparingInt(Wn::idx));
    List<String> main = new ArrayList<>();
    for (Wn x : wn) {
      main.add(x.val());
      if (main.size() == expected) break;
    }

    return new ParseResult(List.copyOf(main), List.copyOf(extraNums), Map.copyOf(attrs));
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
            case "LOTTO" -> 6;
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
              "FL_APIM",
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
              "FL_APIM",
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

  private static LocalDate parseFloridaDate(String raw) {
    if (raw == null || raw.isBlank()) return null;
    String datePart = raw.length() >= 10 ? raw.substring(0, 10) : raw;
    try {
      var f = java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);
      return LocalDate.parse(datePart, f);
    } catch (Exception ignored) {
      try {
        return LocalDate.parse(datePart);
      } catch (Exception ex) {
        return null;
      }
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaEntryDto(
      @JsonProperty("GameName") String gameName,
      @JsonProperty("DrawDate") String drawDate,
      @JsonProperty("DrawType") String drawType,
      @JsonProperty("DrawNumbers") List<FloridaNumberDto> drawNumbers) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaNumberDto(
      @JsonProperty("NumberPick") String numberPick,
      @JsonProperty("NumberType") String numberType) {}
}
