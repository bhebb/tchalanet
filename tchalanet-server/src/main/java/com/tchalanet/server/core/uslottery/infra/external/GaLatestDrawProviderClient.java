package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.uslottery.application.port.out.ProviderDrawQuery;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderClient;
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
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.ga",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class GaLatestDrawProviderClient implements UsLotteryProviderClient {

  private static final String ORIGIN = "GA_API";
  private static final String SHAPE = "GA/latest/v2";

  private final RestClient rest;
  private final UsLotteryProperties props;
  private final JsonbUtils json;
  private final UsLotteryProviderRawCache cache;

  public GaLatestDrawProviderClient(
      @Qualifier("gaLotteryRestClient") RestClient rest,
      UsLotteryProperties props,
      JsonbUtils json,
      UsLotteryProviderRawCache cache) {
    this.rest = Objects.requireNonNull(rest);
    this.props = Objects.requireNonNull(props);
    this.json = Objects.requireNonNull(json);
    this.cache = Objects.requireNonNull(cache);
  }

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.GA;
  }

  @Override
  public List<LatestDraw> fetchDraws(ProviderDrawQuery query) {
    Objects.requireNonNull(query, "query required");

    var cfg = props.getProviders() != null ? props.getProviders().get("ga") : null;
    if (cfg == null || blank(cfg.getLatestPath())) return List.of();

    ZoneId zone = ZoneId.of(blank(cfg.getTimezone()) ? "America/New_York" : cfg.getTimezone());
    String url = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath());

    List<String> codesSorted =
        query.channelCodes() == null ? List.of() : query.channelCodes().stream().sorted().toList();

    String canonical =
        "v2|provider=GA"
            + "|date="
            + query.drawDate()
            + "|shape="
            + SHAPE
            + "|url="
            + url
            + "|codes="
            + (codesSorted.isEmpty() ? "*" : String.join(",", codesSorted))
            + "|max="
            + Math.max(1, query.maxDraws());

    String queryHash = Hashing.sha256Hex(canonical);

    String body =
        cache.getOrFetch(
            provider().name(),
            query.drawDate(),
            queryHash,
            () -> {
              try {
                return rest.get().uri(url).retrieve().body(String.class);
              } catch (Exception ex) {
                log.warn("ga-client fetch failed date={} err={}", query.drawDate(), ex.toString());
                return null;
              }
            });

    if (blank(body)) return List.of();

    String sourceHash = Hashing.sha256Hex(body);
    Instant fetchedAt = Instant.now();

    List<GaEntry> entries = parseEntries(body);
    if (entries.isEmpty()) return List.of();

    List<LatestDraw> out = new ArrayList<>();

    for (GaEntry e : entries) {
      if (e == null) continue;

      String gameKey = normKey(e.gameName());
      if (blank(gameKey)) continue;

      // Map GA game names to PICK3/PICK4 semantics
      String mapped =
          switch (gameKey) {
            case "CASH3", "CASH 3" -> "PICK3";
            case "CASH4", "CASH 4" -> "PICK4";
            default -> gameKey;
          };

      String drawType = norm(e.name());
      GaResult first = (e.results() == null || e.results().isEmpty()) ? null : e.results().get(0);
      if (blank(drawType) && first != null) drawType = norm(first.drawType());
      if (blank(drawType)) continue;

      // Date
      LocalDate date = null;
      if (e.drawTime() != null) {
        try {
          date = Instant.ofEpochMilli(e.drawTime()).atZone(zone).toLocalDate();
        } catch (Exception ignored) {
        }
      }
      if (date == null) date = parseIsoDate(e.drawDate());
      if (date == null) continue;

      String channelCode = mapChannel(mapped, drawType);
      if (channelCode == null) continue;

      List<String> main = parseMain(first);
      if (main.isEmpty()) continue;

      LocalTime time = resolveDrawTime(mapped, drawType);
      OffsetDateTime occurredAt = date.atTime(time).atZone(zone).toOffsetDateTime();

      out.add(
          build(mapped, drawType, channelCode, date, occurredAt, fetchedAt, main, sourceHash, url));
    }

    return filterAndLimit(out, query);
  }

  private LatestDraw build(
      String gameKey,
      String drawType,
      String channelCode,
      LocalDate date,
      OffsetDateTime occurredAt,
      Instant fetchedAt,
      List<String> digits,
      String sourceHash,
      String url) {

    int expected = "PICK3".equals(gameKey) ? 3 : ("PICK4".equals(gameKey) ? 4 : digits.size());

    try {
      DrawMain main = new DrawMain(digits);
      main.requireSize(expected, gameKey + "_" + drawType);

      return new LatestDraw(
          provider(),
          gameKey,
          drawType,
          channelCode,
          date,
          occurredAt,
          fetchedAt,
          main,
          DrawExtras.empty(),
          ResultQuality.COMPLETE,
          ORIGIN,
          Map.of("hash", sourceHash, "url", url));
    } catch (Exception ex) {
      return new LatestDraw(
          provider(),
          gameKey,
          drawType,
          channelCode,
          date,
          occurredAt,
          fetchedAt,
          new DrawMain(digits),
          DrawExtras.empty(),
          ResultQuality.SUSPECT,
          ORIGIN,
          Map.of("hash", sourceHash, "url", url, "error", ex.getMessage()));
    }
  }

  private static List<String> parseMain(GaResult r) {
    if (r == null || r.primary() == null) return List.of();
    List<String> out = new ArrayList<>();
    for (String s : r.primary()) {
      if (s == null) continue;
      String t = s.trim();
      if (!t.isEmpty()) out.add(t);
    }
    return List.copyOf(out);
  }

  private static LocalTime resolveDrawTime(String gameKey, String drawType) {
    // MVP: approximations
    return switch (gameKey) {
      case "PICK3", "PICK4" ->
          "MIDDAY".equals(drawType) ? LocalTime.of(12, 29) : LocalTime.of(19, 29);
      default -> LocalTime.MIDNIGHT;
    };
  }

  private static String mapChannel(String gameKey, String drawType) {
    return switch (gameKey) {
      case "PICK3" ->
          "MIDDAY".equals(drawType)
              ? "US_GA_CASH3_MID"
              : (isEvening(drawType) ? "US_GA_CASH3_EVE" : null);
      case "PICK4" ->
          "MIDDAY".equals(drawType)
              ? "US_GA_CASH4_MID"
              : (isEvening(drawType) ? "US_GA_CASH4_EVE" : null);
      default -> null;
    };
  }

  private static boolean isEvening(String drawType) {
    return "EVENING".equals(drawType) || "NIGHT".equals(drawType);
  }

  private List<GaEntry> parseEntries(String body) {
    try {
      JsonNode root = json.readTree(body);
      if (root != null && root.isArray()) {
        return json.fromJson(body, new TypeReference<List<GaEntry>>() {});
      }
      JsonNode node = root == null ? null : root.get("results");
      if (node != null && node.isArray()) {
        return json.convertValue(node, new TypeReference<List<GaEntry>>() {});
      }
    } catch (Exception ex) {
      log.warn("ga-client parse failed: {}", ex.toString());
    }
    return List.of();
  }

  private static List<LatestDraw> filterAndLimit(List<LatestDraw> all, ProviderDrawQuery query) {
    if (all == null || all.isEmpty()) return List.of();
    var wantedDate = query.drawDate();
    Set<String> wantedCodes = normalizeSet(query.channelCodes());
    var stream = all.stream().filter(Objects::nonNull).filter(d -> wantedDate.equals(d.drawDate()));
    if (!wantedCodes.isEmpty())
      stream = stream.filter(d -> wantedCodes.contains(norm(d.channelCode())));
    return stream.limit(Math.max(1, query.maxDraws())).toList();
  }

  private static Set<String> normalizeSet(Set<String> s) {
    if (s == null || s.isEmpty()) return Set.of();
    var out = new LinkedHashSet<String>();
    for (String x : s) out.add(norm(x));
    return out;
  }

  private static LocalDate parseIsoDate(String raw) {
    if (blank(raw)) return null;
    try {
      String s = raw.trim();
      String first = s.length() >= 10 ? s.substring(0, 10) : s;
      return LocalDate.parse(first);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String joinUrl(String base, String path) {
    String b = blank(base) ? "" : base.trim();
    String p = blank(path) ? "" : path.trim();
    if (b.endsWith("/") && p.startsWith("/")) return b.substring(0, b.length() - 1) + p;
    if (!b.endsWith("/") && !p.startsWith("/") && !b.isBlank() && !p.isBlank()) return b + "/" + p;
    return b + p;
  }

  private static String normKey(String s) {
    if (s == null) return null;
    return s.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  private static String norm(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  private static boolean blank(String s) {
    return s == null || s.trim().isEmpty();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record GaEntry(
      @JsonProperty("drawDate") String drawDate,
      @JsonProperty("gameName") String gameName,
      @JsonProperty("name") String name,
      @JsonProperty("drawTime") Long drawTime,
      @JsonProperty("results") List<GaResult> results) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record GaResult(
      @JsonProperty("primary") List<String> primary, @JsonProperty("drawType") String drawType) {}
}
