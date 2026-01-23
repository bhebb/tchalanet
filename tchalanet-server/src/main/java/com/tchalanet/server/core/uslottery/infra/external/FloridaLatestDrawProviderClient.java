package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.uslottery.application.port.out.ProviderDrawQuery;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.DrawExtras;
import com.tchalanet.server.core.uslottery.domain.model.DrawMain;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.infra.cache.ProviderQueryHash;
import com.tchalanet.server.core.uslottery.infra.cache.UsLotteryProviderRawCache;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.fl",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class FloridaLatestDrawProviderClient implements UsLotteryProviderClient {

  private static final String ORIGIN = "FL_APIM";
  private static final String SHAPE = "FL/latest/v2";

  private final RestClient rest;
  private final UsLotteryProperties props;
  private final JsonbUtils json;
  private final UsLotteryProviderRawCache cache;

  public FloridaLatestDrawProviderClient(
      @Qualifier("floridaLotteryRestClient") RestClient rest,
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
    return UsLotteryProvider.FL;
  }

  @Override
  public List<LatestDraw> fetchDraws(ProviderDrawQuery query) {
    Objects.requireNonNull(query, "query required");

    var cfg = props.getProviders() != null ? props.getProviders().get("fl") : null;
    if (cfg == null || blank(cfg.getLatestPath())) return List.of();

    ZoneId zone = ZoneId.of(blank(cfg.getTimezone()) ? "America/New_York" : cfg.getTimezone());

    String url = joinUrl(cfg.getBaseUrl(), cfg.getLatestPath());

    // hash stable
    List<String> codesSorted =
        query.channelCodes() == null ? List.of() : query.channelCodes().stream().sorted().toList();
    String queryHash = ProviderQueryHash.of("fl", query.drawDate(), codesSorted, SHAPE + "|" + url);

    String body =
        cache.getOrFetch(
            provider().name(),
            query.drawDate(),
            queryHash,
            () -> {
              try {
                return rest.get().uri(url).retrieve().body(String.class);
              } catch (Exception ex) {
                log.warn("fl-client fetch failed date={} err={}", query.drawDate(), ex.toString());
                return null;
              }
            });

    if (blank(body)) return List.of();

    String sourceHash = Hashing.sha256Hex(body);
    Instant fetchedAt = Instant.now();

    List<FloridaEntry> entries = parseEntries(body);
    if (entries.isEmpty()) return List.of();

    List<LatestDraw> out = new ArrayList<>();

    for (FloridaEntry e : entries) {
      if (e == null) continue;

      String gameKey = normKey(e.gameName());
      if (blank(gameKey)) continue;

      LocalDate date = parseFloridaDate(e.drawDate());
      if (date == null) continue;

      // Normalize drawType (FL: MIDDAY/EVENING, LOTTO sometimes empty)
      String drawType = norm(e.drawType());
      if ("LOTTO".equals(gameKey) && blank(drawType)) drawType = "DEFAULT";
      if (!"LOTTO".equals(gameKey) && blank(drawType)) continue;

      String channelCode = mapChannel(gameKey, drawType);
      if (channelCode == null) continue;

      ParseResult pr = parseNumbers(e.drawNumbers(), gameKey);
      if (pr.main().isEmpty()) continue;

      LocalTime drawTime = resolveDrawTime(gameKey, drawType);
      OffsetDateTime occurredAt = date.atTime(drawTime).atZone(zone).toOffsetDateTime();

      DrawExtras extras =
          pr.extras().isEmpty() && pr.attrs().isEmpty()
              ? DrawExtras.empty()
              : new DrawExtras(pr.extras(), pr.attrs());

      out.add(
          build(
              gameKey,
              drawType,
              channelCode,
              date,
              occurredAt,
              fetchedAt,
              pr.main(),
              extras,
              sourceHash,
              url));
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
      List<String> mainDigits,
      DrawExtras extras,
      String sourceHash,
      String url) {

    int expected =
        switch (gameKey) {
          case "PICK3" -> 3;
          case "PICK4" -> 4;
          case "LOTTO" -> 6;
          default -> mainDigits.size();
        };

    try {
      DrawMain main = new DrawMain(mainDigits);
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
          extras,
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
          new DrawMain(mainDigits),
          extras,
          ResultQuality.SUSPECT,
          ORIGIN,
          Map.of("hash", sourceHash, "url", url, "error", ex.getMessage()));
    }
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

  private List<FloridaEntry> parseEntries(String body) {
    try {
      JsonNode root = json.readTree(body);
      if (root != null && root.isArray()) {
        return json.fromJson(body, new TypeReference<List<FloridaEntry>>() {});
      }
      JsonNode dr = root == null ? null : root.get("DrawResults");
      if (dr != null && dr.isArray()) {
        return json.convertValue(dr, new TypeReference<List<FloridaEntry>>() {});
      }
    } catch (Exception ex) {
      log.warn("fl-client parse failed: {}", ex.toString());
    }
    return List.of();
  }

  private static LocalTime resolveDrawTime(String gameKey, String drawType) {
    // MVP: times approximatifs (à aligner avec tes props si tu veux)
    return switch (gameKey) {
      case "PICK3" -> "MIDDAY".equals(drawType) ? LocalTime.of(13, 30) : LocalTime.of(22, 45);
      case "PICK4" -> "MIDDAY".equals(drawType) ? LocalTime.of(13, 30) : LocalTime.of(22, 45);
      case "LOTTO" -> LocalTime.of(23, 15);
      default -> LocalTime.MIDNIGHT;
    };
  }

  private static String mapChannel(String gameKey, String drawType) {
    return switch (gameKey) {
      case "PICK3" ->
          "MIDDAY".equals(drawType)
              ? "US_FL_PICK3_MID"
              : ("EVENING".equals(drawType) ? "US_FL_PICK3_EVE" : null);
      case "PICK4" ->
          "MIDDAY".equals(drawType)
              ? "US_FL_PICK4_MID"
              : ("EVENING".equals(drawType) ? "US_FL_PICK4_EVE" : null);
      case "LOTTO" -> "US_FL_LOTTO";
      default -> null;
    };
  }

  private record ParseResult(List<String> main, List<Integer> extras, Map<String, String> attrs) {}

  private static ParseResult parseNumbers(List<FloridaNumber> drawNumbers, String gameKey) {
    if (drawNumbers == null || drawNumbers.isEmpty())
      return new ParseResult(List.of(), List.of(), Map.of());

    int expected =
        switch (gameKey) {
          case "PICK3" -> 3;
          case "PICK4" -> 4;
          case "LOTTO" -> 6;
          default -> 0;
        };

    record Wn(int idx, String val) {}
    List<Wn> wn = new ArrayList<>();
    List<Integer> extra = new ArrayList<>();
    Map<String, String> attrs = new LinkedHashMap<>();

    for (FloridaNumber n : drawNumbers) {
      if (n == null) continue;
      String pick = safe(n.numberPick());
      if (pick.isBlank()) continue;
      String type = safe(n.numberType()).toLowerCase(Locale.ROOT);

      if (type.startsWith("wn")) {
        int idx = 99;
        try {
          idx = Integer.parseInt(type.substring(2));
        } catch (Exception ignored) {
        }
        wn.add(new Wn(idx, pick.trim()));
      } else {
        // extras
        try {
          extra.add(Integer.parseInt(pick.trim()));
        } catch (Exception ignored) {
        }
        attrs.merge(type.isBlank() ? "extra" : type, pick.trim(), (a, b) -> a + "," + b);
      }
    }

    wn.sort(Comparator.comparingInt(Wn::idx));
    List<String> main = new ArrayList<>();
    for (Wn x : wn) {
      main.add(x.val());
      if (expected > 0 && main.size() >= expected) break;
    }

    return new ParseResult(List.copyOf(main), List.copyOf(extra), Map.copyOf(attrs));
  }

  private static String joinUrl(String base, String path) {
    String b = blank(base) ? "" : base.trim();
    String p = blank(path) ? "" : path.trim();
    if (b.endsWith("/") && p.startsWith("/")) return b.substring(0, b.length() - 1) + p;
    if (!b.endsWith("/") && !p.startsWith("/") && !b.isBlank() && !p.isBlank()) return b + "/" + p;
    return b + p;
  }

  private static LocalDate parseFloridaDate(String raw) {
    if (blank(raw)) return null;

    // FL often "MM/dd/yyyy ..." (we keep first 10 chars when possible)
    String s = raw.trim();
    String first = s.length() >= 10 ? s.substring(0, 10) : s;

    // Try US format first
    try {
      return LocalDate.parse(first, DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US));
    } catch (Exception ignored) {
    }

    // Fallback ISO
    try {
      return LocalDate.parse(first);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static Set<String> normalizeSet(Set<String> s) {
    if (s == null || s.isEmpty()) return Set.of();
    var out = new LinkedHashSet<String>();
    for (String x : s) out.add(norm(x));
    return out;
  }

  private static String normKey(String s) {
    if (s == null) return null;
    return s.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  private static String norm(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }

  private static boolean blank(String s) {
    return s == null || s.trim().isEmpty();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaEntry(
      @JsonProperty("GameName") String gameName,
      @JsonProperty("DrawDate") String drawDate,
      @JsonProperty("DrawType") String drawType,
      @JsonProperty("DrawNumbers") List<FloridaNumber> drawNumbers) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaNumber(
      @JsonProperty("NumberPick") String numberPick,
      @JsonProperty("NumberType") String numberType) {}
}
