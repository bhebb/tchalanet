package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.application.port.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.DrawExtras;
import com.tchalanet.server.core.uslottery.domain.model.DrawMain;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.ny",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class NyLatestDrawProviderClient implements LatestDrawProviderClient {

  private static final String ORIGIN = "NY_OPEN_DATA";

  private final WebClient nyLotteryWebClient;
  private final UsLotteryProperties props;

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.NY;
  }

  @Override
  public List<LatestDraw> fetchLatestDraws() {
    List<LatestDraw> results = new ArrayList<>();

    var providerCfg = props.getProviders() != null ? props.getProviders().get("ny") : null;
    String tz = providerCfg != null ? providerCfg.getTimezone() : "America/New_York";
    String appToken = providerCfg != null ? providerCfg.getAppToken() : null;
    ZoneId zone = ZoneId.of(tz);

    try {
      NyResultDto[] response =
          nyLotteryWebClient
              .get()
              .uri(
                  uriBuilder -> {
                    var b =
                        uriBuilder.queryParam("$limit", 7).queryParam("$order", "draw_date DESC");
                    if (appToken != null && !appToken.isBlank())
                      b = b.queryParam("app_token", appToken);
                    return b.build();
                  })
              .retrieve()
              .bodyToMono(NyResultDto[].class)
              .block();

      if (response == null) return results;

      Instant fetchedAt = Instant.now();

      for (NyResultDto row : response) {
        LocalDate date = LocalDate.parse(row.drawDate().substring(0, 10));
        OffsetDateTime occurredAtUtc = date.atStartOfDay(zone).toOffsetDateTime();

        // NUMBERS (3 digits)
        addIfValid(
            results,
            UsLotteryProvider.NY,
            "NUMBERS",
            "MIDDAY",
            "US_NY_NUM3_MID",
            date,
            occurredAtUtc,
            fetchedAt,
            splitDigits(row.middayDaily()),
            3,
            ORIGIN);

        addIfValid(
            results,
            UsLotteryProvider.NY,
            "NUMBERS",
            "EVENING",
            "US_NY_NUM3_EVE",
            date,
            occurredAtUtc,
            fetchedAt,
            splitDigits(row.eveningDaily()),
            3,
            ORIGIN);

        // WIN4 (4 digits)
        addIfValid(
            results,
            UsLotteryProvider.NY,
            "WIN4",
            "MIDDAY",
            "US_NY_NUM4_MID",
            date,
            occurredAtUtc,
            fetchedAt,
            splitDigits(row.middayWin4()),
            4,
            ORIGIN);

        addIfValid(
            results,
            UsLotteryProvider.NY,
            "WIN4",
            "EVENING",
            "US_NY_NUM4_EVE",
            date,
            occurredAtUtc,
            fetchedAt,
            splitDigits(row.eveningWin4()),
            4,
            ORIGIN);
      }

    } catch (Exception e) {
      log.warn("ny-latest-client: failed: {}", e.toString());
    }

    return results;
  }

  private void addIfValid(
      List<LatestDraw> out,
      UsLotteryProvider provider,
      String externalGameKey,
      String externalDrawType,
      String channelCode,
      LocalDate date,
      OffsetDateTime occurredAtUtc,
      Instant fetchedAtUtc,
      List<String> digits,
      int expectedSize,
      String origin) {
    if (digits == null || digits.isEmpty()) return;
    try {
      DrawMain main = new DrawMain(digits);
      main.requireSize(expectedSize, externalGameKey + "_" + externalDrawType);

      out.add(
          new LatestDraw(
              provider,
              externalGameKey,
              externalDrawType,
              channelCode,
              date,
              occurredAtUtc,
              fetchedAtUtc,
              main,
              DrawExtras.empty(),
              ResultQuality.COMPLETE,
              origin,
              Map.of()));
    } catch (Exception ex) {
      // tu peux ignorer si tu veux (simple) ; ici je garde une trace SUSPECT
      out.add(
          new LatestDraw(
              provider,
              externalGameKey,
              externalDrawType,
              channelCode,
              date,
              occurredAtUtc,
              fetchedAtUtc,
              new DrawMain(digits),
              DrawExtras.empty(),
              ResultQuality.SUSPECT,
              origin,
              Map.of("error", ex.getMessage())));
    }
  }

  private List<String> splitDigits(String s) {
    if (s == null) return List.of();
    String trimmed = s.trim();
    if (trimmed.isEmpty()) return List.of();
    return Arrays.stream(trimmed.split("")).filter(p -> !p.isBlank()).toList();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NyResultDto(
      @JsonProperty("draw_date") String drawDate,
      @JsonProperty("midday_daily") String middayDaily,
      @JsonProperty("evening_daily") String eveningDaily,
      @JsonProperty("midday_win_4") String middayWin4,
      @JsonProperty("evening_win_4") String eveningWin4) {}
}
