package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
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
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.florida",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class FloridaLatestDrawProviderClient implements LatestDrawProviderClient {

  private final WebClient floridaLotteryWebClient;
  private final UsLotteryProperties props;
  private final ObjectMapper objectMapper;

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.FLORIDA;
  }

  @Override
  public List<LatestDraw> fetchLatestDraws() {
    List<LatestDraw> results = new ArrayList<>();

    var providerCfg = props.getProviders() != null ? props.getProviders().get("florida") : null;
    String tz = providerCfg != null ? providerCfg.getTimezone() : "America/New_York";
    String latestPath =
        providerCfg != null ? providerCfg.getLatestPath() : "/drawgamesapp/getLatestDrawGames";
    ZoneId zone = ZoneId.of(tz);

    try {
      // Fetch raw body as String because provider may return either an object { DrawResults: [...]
      // }
      // or a raw array [ {...}, {...} ]
      String body =
          floridaLotteryWebClient.get().uri(latestPath).retrieve().bodyToMono(String.class).block();

      if (body == null || body.isBlank()) return results;

      // parse into JsonNode to detect array vs object
      JsonNode root = objectMapper.readTree(body);
      log.debug(
          "fl-latest-client: fetched body length={} rootType={}",
          body.length(),
          root.isArray() ? "array" : root.isObject() ? "object" : root.getNodeType());

      // Parse into flat entries where each entry represents one draw row
      List<FloridaEntryDto> entries = new ArrayList<>();

      if (root.isArray()) {
        entries = objectMapper.readValue(body, new TypeReference<>() {});
      } else if (root.isObject()) {
        JsonNode dr = root.get("DrawResults");
        if (dr != null && dr.isArray()) {
          entries = objectMapper.convertValue(dr, new TypeReference<>() {});
        } else {
          entries = List.of();
        }
      }

      if (entries == null || entries.isEmpty()) {
        log.debug("fl-latest-client: no entries parsed from provider (path={})", latestPath);
        return results;
      }

      Instant fetchedAt = Instant.now();
      log.debug("fl-latest-client: parsed {} entries from provider", entries.size());

      for (FloridaEntryDto e : entries) {
        processEntry(results, e, latestPath, zone, fetchedAt);
      }
    } catch (Exception e) {
      log.warn("fl-latest-client: failed: {}", e.toString());
    }

    return results;
  }

  // Parse result holder
  private record ParseResult(
      List<String> main, List<Integer> extraNumbers, Map<String, String> attributes) {}

  private void processEntry(
      List<LatestDraw> results,
      FloridaEntryDto e,
      String latestPath,
      ZoneId zone,
      Instant fetchedAt) {
    String gameKey = normalizeGameKey(e.gameName()); // "PICK3" / "PICK4"
    if (!("PICK3".equals(gameKey) || "PICK4".equals(gameKey) || "LOTTO".equals(gameKey))) return;

    String drawType = normalize(e.drawType()); // MIDDAY / EVENING
    if (drawType == null) return;

    LocalDate date = parseFloridaDate(e.drawDate());
    if (date == null) return;

    String channelCode = mapChannelCode(gameKey, drawType);
    if (channelCode == null) return;

    ParseResult pr = parseDigits(e.drawNumbers(), gameKey);
    if (pr.main().isEmpty()) return;

    OffsetDateTime occurredAt = date.atStartOfDay(zone).toOffsetDateTime();

    DrawExtras extras = new DrawExtras(pr.extraNumbers(), pr.attributes());

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
        latestPath);
  }

  private String normalizeGameKey(String gameName) {
    if (gameName == null) return null;
    return gameName.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  private LocalDate parseFloridaDate(String raw) {
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

  private ParseResult parseDigits(List<FloridaNumberDto> drawNumbers, String gameKey) {
    if (drawNumbers == null || drawNumbers.isEmpty())
      return new ParseResult(List.of(), List.of(), Map.of());

    int expected = "PICK3".equals(gameKey) ? 3 : 4;

    record Wn(int idx, String val) {}
    List<Wn> wn = new ArrayList<>();

    List<Integer> extraNums = new ArrayList<>();
    Map<String, String> attrs = new HashMap<>();

    for (FloridaNumberDto n : drawNumbers) {
      if (n == null) continue;
      String type = n.numberType();
      String pickRaw = n.numberPick();
      String pickTrim = pickRaw == null ? "" : pickRaw.trim();
      if (type == null) {
        // treat as extra if no type
        if (!pickTrim.isBlank()) {
          try {
            extraNums.add(Integer.parseInt(pickTrim));
          } catch (NumberFormatException ignored) {
          }
        }
        continue;
      }

      String t = type.trim().toLowerCase(Locale.ROOT);
      if (!t.startsWith("wn")) {
        // extra
        if (!pickTrim.isBlank()) {
          try {
            extraNums.add(Integer.parseInt(pickTrim));
          } catch (NumberFormatException ignored) {
          }
          if (attrs.containsKey(t)) attrs.put(t, attrs.get(t) + "," + pickTrim);
          else attrs.put(t, pickTrim);
        }
        continue;
      }

      int idx = 99;
      try {
        idx = Integer.parseInt(t.substring(2));
      } catch (Exception ignored) {
      }
      if (!pickTrim.isEmpty()) wn.add(new Wn(idx, pickTrim));
    }

    wn.sort(Comparator.comparingInt(Wn::idx));
    List<String> main = new ArrayList<>();
    for (Wn x : wn) {
      main.add(x.val());
      if (main.size() == expected) break;
    }

    return new ParseResult(List.copyOf(main), List.copyOf(extraNums), Map.copyOf(attrs));
  }

  private static String normalize(String s) {
    if (s == null) return null;
    String t = s.trim().toUpperCase();
    if (t.isBlank()) return null;
    return t;
  }

  private String mapChannelCode(String externalGameKey, String externalDrawType) {
    return switch (externalGameKey + "_" + externalDrawType) {
      case "PICK3_MIDDAY" -> "US_FL_PICK3_MID";
      case "PICK3_EVENING" -> "US_FL_PICK3_EVE";
      case "PICK4_MIDDAY" -> "US_FL_PICK4_MID";
      case "PICK4_EVENING" -> "US_FL_PICK4_EVE";
      default -> null;
    };
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
      DrawExtras extras,
      String latestPath) {
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
              extras,
              ResultQuality.COMPLETE,
              "FL_APIM",
              Map.of("path", latestPath)));
    } catch (Exception ex) {
      log.debug(
          "florida: suspect row ignored: game={} type={} date={} err= {}",
          externalGameKey,
          externalDrawType,
          date,
          ex.toString());
      results.add(
          new LatestDraw(
              UsLotteryProvider.FLORIDA,
              externalGameKey,
              externalDrawType,
              channelCode,
              date,
              occurredAtUtc,
              fetchedAt,
              new DrawMain(digits), // may throw if digits invalid; otherwise ok
              extras,
              ResultQuality.SUSPECT,
              "FL_APIM",
              Map.of("path", latestPath, "error", ex.getMessage())));
    }
  }

  // ---- DTOs ----
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaEntryDto(
      @JsonProperty("GameName") String gameName,
      @JsonProperty("DrawDate") String drawDate,
      @JsonProperty("DrawType") String drawType,
      @JsonProperty("DrawNumbers") List<FloridaNumberDto> drawNumbers) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaNumberDto(
      @JsonProperty("NumberPick") String numberPick, // String to preserve leading zeros
      @JsonProperty("NumberType") String numberType) {}
}
