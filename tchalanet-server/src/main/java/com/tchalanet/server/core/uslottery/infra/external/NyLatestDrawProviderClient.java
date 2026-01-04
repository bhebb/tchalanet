package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.uslottery.application.port.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.DrawExtras;
import com.tchalanet.server.core.uslottery.domain.model.DrawMain;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.infra.cache.UsLotteryProviderRawCache;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
public class NyLatestDrawProviderClient implements LatestDrawProviderClient {

  private static final String ORIGIN = "NY_OPEN_DATA";
  private final RestClient nyLotteryRestClient;
  private final UsLotteryProperties props;
  private final UsLotteryProviderRawCache cacheManager;
  private final JsonbUtils jsonbUtils;

  public NyLatestDrawProviderClient(
      @Qualifier("nyLotteryRestClient") RestClient nyLotteryRestClient,
      UsLotteryProperties props,
      UsLotteryProviderRawCache cacheManager,
      JsonbUtils jsonbUtils) {
    this.nyLotteryRestClient = nyLotteryRestClient;
    this.props = props;
    this.cacheManager = cacheManager;
    this.jsonbUtils = jsonbUtils;
  }

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.NY;
  }

  @Override
  public List<LatestDraw> fetchLatestDraws() {
    var providerCfg = props.getProviders() != null ? props.getProviders().get("ny") : null;
    var tz = providerCfg != null ? providerCfg.getTimezone() : "America/New_York";
    var appToken = providerCfg != null ? providerCfg.getAppToken() : null;
    var zone = ZoneId.of(tz);

    // default: fetch all known NY channel codes
    var channelCodes =
        List.of(
            "US_NY_NUM3_MID",
            "US_NY_NUM3_EVE",
            "US_NY_NUM4_MID",
            "US_NY_NUM4_EVE",
            "US_NY_TAKE5_EVE");

    int maxDraws = 500; // default bulk limit
    var drawDate = LocalDate.now(zone);

    // build SOQL
    var select = buildSelect(channelCodes);
    var where = "draw_date <= '" + drawDate + "T23:59:59.000'";

    // query identity: include select + where so cache distinguishes queries
    var queryHash = sha256(select + "|" + where + "|" + maxDraws);

    var body =
        cacheManager.getOrFetch(
            provider().name(),
            drawDate,
            queryHash,
            () -> {
              try {
                return nyLotteryRestClient
                    .get()
                    .uri(
                        (UriBuilder ub) -> {
                          var b =
                              ub.queryParam("$limit", maxDraws)
                                  .queryParam("$select", select)
                                  .queryParam("$where", where)
                                  .queryParam("$order", "draw_date DESC");
                          if (appToken != null && !appToken.isBlank()) {
                            b = b.queryParam("app_token", appToken);
                          }
                          return b.build();
                        })
                    .retrieve()
                    .body(String.class);
              } catch (Exception ex) {
                log.warn("ny-latest-client: fetch failed: {}", ex.getMessage(), ex);
                return null;
              }
            });

    if (StringUtils.isBlank(body)) {
      return List.of();
    }

    return parseNyBody(body, zone);
  }

  private List<LatestDraw> parseNyBody(String body, ZoneId zone) {
    List<LatestDraw> out = new ArrayList<>();
    try {
      NyResultDto[] rows = jsonbUtils.fromJson(body, NyResultDto[].class);

      if (rows == null) return List.of();

      var hash = sha256(body);
      var fetchedAt = Instant.now();

      for (var row : rows) {
        if (row == null || row.drawDate() == null || row.drawDate().length() < 10) continue;

        var date = LocalDate.parse(row.drawDate().substring(0, 10));
        var occurredAtUtc = date.atStartOfDay(zone).toOffsetDateTime();

        // NUM3
        addIfValid(
            out,
            "NUMBERS",
            "MIDDAY",
            "US_NY_NUM3_MID",
            date,
            occurredAtUtc,
            fetchedAt,
            splitDigits(row.middayDaily()),
            3,
            hash);
        addIfValid(
            out,
            "NUMBERS",
            "EVENING",
            "US_NY_NUM3_EVE",
            date,
            occurredAtUtc,
            fetchedAt,
            splitDigits(row.eveningDaily()),
            3,
            hash);

        // NUM4
        addIfValid(
            out,
            "WIN4",
            "MIDDAY",
            "US_NY_NUM4_MID",
            date,
            occurredAtUtc,
            fetchedAt,
            splitDigits(row.middayWin4()),
            4,
            hash);
        addIfValid(
            out,
            "WIN4",
            "EVENING",
            "US_NY_NUM4_EVE",
            date,
            occurredAtUtc,
            fetchedAt,
            splitDigits(row.eveningWin4()),
            4,
            hash);
      }
    } catch (Exception e) {
      log.warn("ny-latest-client: parse failed: {}", e.toString());
    }
    return out;
  }

  private void addIfValid(
      List<LatestDraw> out,
      String externalGameKey,
      String externalDrawType,
      String channelCode,
      LocalDate date,
      OffsetDateTime occurredAtUtc,
      Instant fetchedAtUtc,
      List<String> digits,
      int expectedSize,
      String sourceHash) {

    if (digits == null || digits.isEmpty()) return;

    try {
      var main = new DrawMain(digits);
      main.requireSize(expectedSize, externalGameKey + "_" + externalDrawType);

      out.add(
          new LatestDraw(
              provider(),
              externalGameKey,
              externalDrawType,
              channelCode,
              date,
              occurredAtUtc,
              fetchedAtUtc,
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
              occurredAtUtc,
              fetchedAtUtc,
              new DrawMain(digits),
              DrawExtras.empty(),
              ResultQuality.SUSPECT,
              ORIGIN,
              Map.of("hash", sourceHash, "error", ex.getMessage())));
    }
  }

  // ---------- SOQL helpers ----------

  private static String buildSelect(List<String> channelCodes) {
    // always need draw_date
    var cols = new LinkedHashSet<String>();
    cols.add("draw_date");

    for (String cc : channelCodes) {
      switch (cc) {
        case "US_NY_NUM3_MID" -> cols.add("midday_daily");
        case "US_NY_NUM3_EVE" -> cols.add("evening_daily");
        case "US_NY_NUM4_MID" -> cols.add("midday_win_4");
        case "US_NY_NUM4_EVE" -> cols.add("evening_win_4");
        default -> {
          /* ignore non-NY codes */
        }
      }
    }

    return String.join(",", cols);
  }

  private static String selectSignature(List<String> channelCodes) {
    // stable signature for cache key (sorted)
    var ny = channelCodes.stream().filter(c -> c.startsWith("US_NY_")).sorted().toList();
    return String.join("|", ny);
  }

  private static List<String> normalizeCodes(List<String> codes) {
    if (codes == null) return List.of();
    return codes.stream()
        .filter(Objects::nonNull)
        .map(s -> s.trim().toUpperCase(Locale.ROOT))
        .filter(s -> !s.isBlank())
        .distinct()
        .toList();
  }

  private static int clamp(int v, int min, int max) {
    return Math.max(min, Math.min(max, v));
  }

  private static List<String> splitDigits(String s) {
    if (s == null) return List.of();
    String trimmed = s.trim();
    if (trimmed.isEmpty()) return List.of();
    return Arrays.stream(trimmed.split("")).filter(p -> !p.isBlank()).toList();
  }

  private static String sha256(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(dig.length * 2);
      for (byte b : dig) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      return "NO_HASH";
    }
  }

  private static String buildAbsoluteUri(String baseUrl, String pathOrQuery) {
    if (pathOrQuery == null) throw new IllegalArgumentException("pathOrQuery is null");
    String trimmed = pathOrQuery.trim();
    if (trimmed.matches("^[a-zA-Z][a-zA-Z0-9+\\-.]*://.*")) return trimmed;

    String base = (baseUrl == null || baseUrl.isBlank()) ? "https://data.ny.gov" : baseUrl.trim();
    if (!base.matches("^[a-zA-Z][a-zA-Z0-9+\\-.]*://.*")) {
      base = "https://" + base;
    }

    // if pathOrQuery starts with '?', append to base
    if (trimmed.startsWith("?")) {
      return base + (base.contains("?") ? "&" : "") + trimmed.substring(1);
    }

    // else ensure single slash between base and path/query
    if (base.endsWith("/") && trimmed.startsWith("/")) {
      return base + trimmed.substring(1);
    } else if (!base.endsWith("/") && !trimmed.startsWith("/")) {
      return base + "/" + trimmed;
    } else {
      return base + trimmed;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NyResultDto(
      @JsonProperty("draw_date") String drawDate,
      @JsonProperty("midday_daily") String middayDaily,
      @JsonProperty("evening_daily") String eveningDaily,
      @JsonProperty("midday_win_4") String middayWin4,
      @JsonProperty("evening_win_4") String eveningWin4) {}
}
