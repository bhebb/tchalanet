package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

@Component
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.ny",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class NyLatestDrawProviderClient implements UsLotteryProviderClient {

  private static final String ORIGIN = "NY_OPEN_DATA";
  private static final String QUERY_SHAPE = "NY/soql/v2";

  private final RestClient nyRestClient;
  private final UsLotteryProperties props;
  private final UsLotteryProviderRawCache cache;
  private final JsonbUtils json;

  public NyLatestDrawProviderClient(
      @Qualifier("nyLotteryRestClient") RestClient nyRestClient,
      UsLotteryProperties props,
      UsLotteryProviderRawCache cache,
      JsonbUtils json) {
    this.nyRestClient = Objects.requireNonNull(nyRestClient);
    this.props = Objects.requireNonNull(props);
    this.cache = Objects.requireNonNull(cache);
    this.json = Objects.requireNonNull(json);
  }

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.NY;
  }

  @Override
  public List<LatestDraw> fetchDraws(ProviderDrawQuery query) {
    Objects.requireNonNull(query, "query required");

    var cfg = props.getProviders() != null ? props.getProviders().get("ny") : null;
    if (cfg == null || !cfg.isEnabled()) return List.of();

    ZoneId zone = ZoneId.of(blank(cfg.getTimezone()) ? "America/New_York" : cfg.getTimezone());
    String appToken = cfg.getAppToken();

    // If caller didn't specify channels, take default
    Set<String> wantedCodes =
        (query.channelCodes() == null || query.channelCodes().isEmpty())
            ? new LinkedHashSet<>(defaultNyCodes())
            : new LinkedHashSet<>(query.channelCodes());

    int max = clamp(query.maxDraws(), 1, 500);

    // SOQL select only required cols
    String select = buildSelect(wantedCodes);
    String where = "draw_date <= '" + query.drawDate() + "T23:59:59.000'";

    String canonical =
        "v2|provider=NY"
            + "|date="
            + query.drawDate()
            + "|shape="
            + QUERY_SHAPE
            + "|select="
            + select
            + "|where="
            + where
            + "|limit="
            + max
            + "|token="
            + (StringUtils.isBlank(appToken) ? "0" : "1")
            + "|codes="
            + String.join(",", wantedCodes.stream().sorted().toList());

    String queryHash = Hashing.sha256Hex(canonical);

    String body =
        cache.getOrFetch(
            provider().name(),
            query.drawDate(),
            queryHash,
            () -> {
              try {
                return nyRestClient
                    .get()
                    .uri(
                        (UriBuilder ub) -> {
                          var b =
                              ub.queryParam("$limit", max)
                                  .queryParam("$select", select)
                                  .queryParam("$where", where)
                                  .queryParam("$order", "draw_date DESC");
                          if (!StringUtils.isBlank(appToken))
                            b = b.queryParam("app_token", appToken);
                          return b.build();
                        })
                    .retrieve()
                    .body(String.class);
              } catch (Exception ex) {
                log.warn("ny-client fetch failed date={} err={}", query.drawDate(), ex.toString());
                return null;
              }
            });

    if (blank(body)) return List.of();

    String sourceHash = Hashing.sha256Hex(body);
    Instant fetchedAt = Instant.now();

    // Parse rows
    NyRow[] rows;
    try {
      rows = json.fromJson(body, NyRow[].class);
    } catch (Exception ex) {
      log.warn("ny-client parse failed: {}", ex.toString());
      return List.of();
    }
    if (rows == null || rows.length == 0) return List.of();

    List<LatestDraw> out = new ArrayList<>(16);

    for (NyRow r : rows) {
      if (r == null || blank(r.drawDate()) || r.drawDate().length() < 10) continue;

      LocalDate date;
      try {
        date = LocalDate.parse(r.drawDate().substring(0, 10));
      } catch (Exception ignored) {
        continue;
      }

      addIfWanted(
          out,
          wantedCodes,
          "US_NY_NUM3_MID",
          date,
          fetchedAt,
          sourceHash,
          "NUMBERS",
          "MIDDAY",
          digits(r.middayDaily()),
          3);
      addIfWanted(
          out,
          wantedCodes,
          "US_NY_NUM3_EVE",
          date,
          fetchedAt,
          sourceHash,
          "NUMBERS",
          "EVENING",
          digits(r.eveningDaily()),
          3);

      addIfWanted(
          out,
          wantedCodes,
          "US_NY_NUM4_MID",
          date,
          fetchedAt,
          sourceHash,
          "WIN4",
          "MIDDAY",
          digits(r.middayWin4()),
          4);
      addIfWanted(
          out,
          wantedCodes,
          "US_NY_NUM4_EVE",
          date,
          fetchedAt,
          sourceHash,
          "WIN4",
          "EVENING",
          digits(r.eveningWin4()),
          4);
    }

    return filterAndLimit(out, query);
  }

  private void addIfWanted(
      List<LatestDraw> out,
      Set<String> wanted,
      String channelCode,
      LocalDate date,
      Instant fetchedAt,
      String sourceHash,
      String externalGameKey,
      String externalDrawType,
      List<String> mainDigits,
      int expected) {

    if (!wanted.isEmpty() && !wanted.contains(channelCode)) return;
    if (mainDigits == null || mainDigits.isEmpty()) return;

    // occurredAt est calculé par l'adapter via OccurredAtResolver (slot.drawTime + slot.timezone)
    OffsetDateTime occurredAt = null;

    try {
      DrawMain main = new DrawMain(mainDigits);
      main.requireSize(expected, externalGameKey + "_" + externalDrawType);

      out.add(
          new LatestDraw(
              provider(),
              externalGameKey,
              externalDrawType,
              channelCode,
              date,
              occurredAt,
              fetchedAt,
              main,
              DrawExtras.empty(),
              ResultQuality.COMPLETE,
              ORIGIN,
              Map.of("hash", sourceHash)));
    } catch (Exception ex) {
      out.add(
          new LatestDraw(
              provider(),
              externalGameKey,
              externalDrawType,
              channelCode,
              date,
              occurredAt,
              fetchedAt,
              new DrawMain(mainDigits),
              DrawExtras.empty(),
              ResultQuality.SUSPECT,
              ORIGIN,
              Map.of("hash", sourceHash, "error", ex.getMessage())));
    }
  }

  private static List<LatestDraw> filterAndLimit(List<LatestDraw> all, ProviderDrawQuery query) {
    if (all == null || all.isEmpty()) return List.of();

    var wantedDate = query.drawDate();
    var wantedCodes = normalizeSet(query.channelCodes());
    var stream = all.stream().filter(Objects::nonNull).filter(d -> wantedDate.equals(d.drawDate()));

    if (!wantedCodes.isEmpty()) {
      stream =
          stream.filter(
              d -> d.channelCode() != null && wantedCodes.contains(norm(d.channelCode())));
    }
    return stream.limit(Math.max(1, query.maxDraws())).toList();
  }

  private static String buildSelect(Set<String> wantedCodes) {
    var cols = new LinkedHashSet<String>();
    cols.add("draw_date");
    for (String cc : wantedCodes) {
      switch (cc) {
        case "US_NY_NUM3_MID" -> cols.add("midday_daily");
        case "US_NY_NUM3_EVE" -> cols.add("evening_daily");
        case "US_NY_NUM4_MID" -> cols.add("midday_win_4");
        case "US_NY_NUM4_EVE" -> cols.add("evening_win_4");
        default -> {
          /* ignore */
        }
      }
    }
    if (cols.size() == 1) { // only draw_date -> still need at least one result col
      cols.add("midday_daily");
      cols.add("evening_daily");
      cols.add("midday_win_4");
      cols.add("evening_win_4");
    }
    return String.join(",", cols);
  }

  private static List<String> defaultNyCodes() {
    return List.of("US_NY_NUM3_MID", "US_NY_NUM3_EVE", "US_NY_NUM4_MID", "US_NY_NUM4_EVE");
  }

  private static List<String> digits(String s) {
    if (blank(s)) return List.of();
    String t = s.trim();
    // "123" => ["1","2","3"]
    return Arrays.stream(t.split("")).filter(p -> !p.isBlank()).toList();
  }

  private static Set<String> normalizeSet(Set<String> s) {
    if (s == null || s.isEmpty()) return Set.of();
    var out = new LinkedHashSet<String>();
    for (String x : s) out.add(norm(x));
    return out;
  }

  private static String norm(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  private static boolean blank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static int clamp(int v, int min, int max) {
    return Math.max(min, Math.min(max, v));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NyRow(
      @JsonProperty("draw_date") String drawDate,
      @JsonProperty("midday_daily") String middayDaily,
      @JsonProperty("evening_daily") String eveningDaily,
      @JsonProperty("midday_win_4") String middayWin4,
      @JsonProperty("evening_win_4") String eveningWin4) {}
}
