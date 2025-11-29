package com.tchalanet.server.core.uslottery.infra.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.core.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.core.uslottery.domain.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.domain.ports.out.LatestDrawProviderClient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Client NY Open Data pour récupérer les derniers tirages DAILY (NUMBERS) / WIN4. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NyLatestDrawProviderClient implements LatestDrawProviderClient {

  private final WebClient nyLotteryWebClient;

  @Value("${tch.us-lottery.ny.timezone:America/New_York}")
  private String nyTimezone;

  @Value("${tch.us-lottery.ny.app-token:}")
  private String appToken;

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.NY;
  }

  @Override
  public List<LatestDrawDto> fetchLatestDraws() {
    List<LatestDrawDto> results = new ArrayList<>();
    try {
      NyResultDto[] response =
          nyLotteryWebClient
              .get()
              .uri(
                  uriBuilder -> {
                    var builder =
                        uriBuilder.queryParam("$limit", 7).queryParam("$order", "draw_date DESC");
                    if (appToken != null && !appToken.isBlank()) {
                      builder = builder.queryParam("app_token", appToken);
                    }
                    return builder.build();
                  })
              .retrieve()
              .bodyToMono(NyResultDto[].class)
              .block();

      if (response == null) {
        return results;
      }
      ZoneId zone = ZoneId.of(nyTimezone);

      for (NyResultDto row : response) {
        LocalDate date = LocalDate.parse(row.drawDate().substring(0, 10));
        OffsetDateTime baseTimeUtc = date.atStartOfDay(zone).toOffsetDateTime();

        // NUMBERS (DAILY) 3 chiffres : midday_daily / evening_daily
        if (row.middayDaily() != null && !row.middayDaily().isBlank()) {
          List<String> numbers = splitDigits(row.middayDaily());
          if (numbers.size() == 3) {
            String resultJson =
                String.format("{\"numbers\":%s,\"source\":\"NY_OPEN_DATA\"}", numbers.toString());
            results.add(new LatestDrawDto("US_NY_NUM3_MID", baseTimeUtc.toInstant(), resultJson));
          }
        }
        if (row.eveningDaily() != null && !row.eveningDaily().isBlank()) {
          List<String> numbers = splitDigits(row.eveningDaily());
          if (numbers.size() == 3) {
            String resultJson =
                String.format("{\"numbers\":%s,\"source\":\"NY_OPEN_DATA\"}", numbers.toString());
            results.add(new LatestDrawDto("US_NY_NUM3_EVE", baseTimeUtc.toInstant(), resultJson));
          }
        }

        // WIN4 : midday_win_4 / evening_win_4 (4 chiffres)
        if (row.middayWin4() != null && !row.middayWin4().isBlank()) {
          List<String> numbers = splitDigits(row.middayWin4());
          if (numbers.size() == 4) {
            String resultJson =
                String.format("{\"numbers\":%s,\"source\":\"NY_OPEN_DATA\"}", numbers.toString());
            results.add(new LatestDrawDto("US_NY_NUM4_MID", baseTimeUtc.toInstant(), resultJson));
          }
        }
        if (row.eveningWin4() != null && !row.eveningWin4().isBlank()) {
          List<String> numbers = splitDigits(row.eveningWin4());
          if (numbers.size() == 4) {
            String resultJson =
                String.format("{\"numbers\":%s,\"source\":\"NY_OPEN_DATA\"}", numbers.toString());
            results.add(new LatestDrawDto("US_NY_NUM4_EVE", baseTimeUtc.toInstant(), resultJson));
          }
        }
      }
    } catch (Exception e) {
      log.warn("ny-latest-client: failed to fetch latest draws: {}", e.toString());
    }
    return results;
  }

  private List<String> splitDigits(String s) {
    if (s == null) return List.of();
    String trimmed = s.trim();
    if (trimmed.isEmpty()) return List.of();
    // ex: "356" -> ["3","5","6"] ; "5931" -> ["5","9","3","1"]
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
