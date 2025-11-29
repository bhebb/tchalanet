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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Client Florida Lottery minimal pour récupérer les derniers tirages Pick 3 / Pick 4. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FloridaLatestDrawProviderClient implements LatestDrawProviderClient {

  private final WebClient floridaLotteryWebClient;

  @Value("${tch.us-lottery.florida.timezone:America/New_York}")
  private String floridaTimezone;

  @Value("${tch.us-lottery.florida.latest-path:/drawgamesapp/getLatestDrawGames}")
  private String latestPath;

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.FLORIDA;
  }

  @Override
  public List<LatestDrawDto> fetchLatestDraws() {
    List<LatestDrawDto> results = new ArrayList<>();
    try {
      FloridaResultDto response =
          floridaLotteryWebClient
              .get()
              .uri(latestPath)
              .retrieve()
              .bodyToMono(FloridaResultDto.class)
              .block();

      if (response == null || response.drawResults == null) {
        return results;
      }

      ZoneId zone = ZoneId.of(floridaTimezone);
      for (FloridaDrawGameDto game : response.drawResults) {
        String gameName = game.gameName(); // ex: "PICK 3", "PICK 4",
        if (gameName == null) continue;
        String externalKey = gameName.trim().toUpperCase().replace(" ", ""); // PICK3, PICK4

        // On ne traite pour l'instant que PICK3 / PICK4 (les autres jeux seront mappés plus tard)
        if (!externalKey.equals("PICK3") && !externalKey.equals("PICK4")) {
          continue;
        }

        for (FloridaDrawDto d : game.draws()) {
          LocalDate date = d.getDrawDate();
          String drawType = d.getDrawType(); // MIDDAY / EVENING / MOR / ...
          if (drawType == null) continue;

          OffsetDateTime drawTimeUtc = date.atStartOfDay(zone).toOffsetDateTime();
          List<String> numbers = parseNumbersFromDrawNumbers(d.drawNumbers());
          if (numbers.isEmpty()) continue;

          String normalizedType = drawType.trim().toUpperCase();
          String channelCode = mapChannelCode(externalKey, normalizedType);
          if (channelCode == null) {
            // ex: CASH POP MOR/AFT/EVE, PICK2/PICK5, etc.
            continue;
          }

          // Build minimal DTO expected by the rest of the pipeline
          String resultJson =
              String.format("{\"numbers\":%s,\"source\":\"FL_APIM\"}", numbers.toString());
          results.add(new LatestDrawDto(channelCode, drawTimeUtc.toInstant(), resultJson));
        }
      }
    } catch (Exception e) {
      log.warn("fl-latest-client: failed to fetch latest draws: {}", e.toString());
    }
    return results;
  }

  private String mapChannelCode(String externalKey, String drawType) {
    // externalKey: PICK3/PICK4 ; drawType: MIDDAY/EVENING (d'après le JSON fourni)
    return switch (externalKey + "_" + drawType) {
      case "PICK3_MIDDAY" -> "US_FL_PICK3_MID";
      case "PICK3_EVENING" -> "US_FL_PICK3_EVE";
      case "PICK4_MIDDAY" -> "US_FL_PICK4_MID";
      case "PICK4_EVENING" -> "US_FL_PICK4_EVE";
      default -> null;
    };
  }

  private List<String> parseNumbersFromDrawNumbers(List<FloridaNumberDto> drawNumbers) {
    if (drawNumbers == null || drawNumbers.isEmpty()) return List.of();
    // On garde uniquement les NumberType wn1/wn2/wn3/wn4/wn5 (ignore fb = Fireball)
    List<String> out = new ArrayList<>();
    for (FloridaNumberDto n : drawNumbers) {
      String type = n.numberType();
      if (type != null && type.startsWith("wn")) {
        out.add(String.valueOf(n.numberPick()));
      }
    }
    return out;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaResultDto(
      @JsonProperty("DrawResults") List<FloridaDrawGameDto> drawResults) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaDrawGameDto(
      @JsonProperty("GameName") String gameName,
      @JsonProperty("Draws") List<FloridaDrawDto> draws) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class FloridaDrawDto {

    @JsonProperty("DrawDate")
    private String drawDate;

    @JsonProperty("DrawType")
    private String drawType; // MIDDAY / EVENING / MOR / AFT / EVE / LAT…

    @JsonProperty("DrawNumbers")
    private List<FloridaNumberDto> drawNumbers;

    LocalDate getDrawDate() {
      return LocalDate.parse(drawDate.substring(0, 10));
    }

    String getDrawType() {
      return drawType;
    }

    List<FloridaNumberDto> drawNumbers() {
      return drawNumbers;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FloridaNumberDto(
      @JsonProperty("NumberPick") int numberPick, @JsonProperty("NumberType") String numberType) {}
}
